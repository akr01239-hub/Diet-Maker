import { round } from '../../calc/anthropometry';
import {
  DayPlan,
  FoodItem,
  Meal,
  MealItem,
  MealSlot,
  MEAL_SLOTS,
  PlanPreferences,
  PlanTargets,
  SLOT_KCAL_WEIGHTS,
  WeekPlan,
} from './food.types';
import { eligibleFoods } from './foodFilter';

const MAIN_SLOTS: MealSlot[] = ['breakfast', 'lunch', 'dinner'];
const MIN_GRAMS = 15;
const MAX_GRAMS = 400;

function proteinDensity(f: FoodItem): number {
  return f.proteinG / Math.max(1, f.kcal);
}

/** Sizes a serving (g) to deliver `kcalTarget`, clamped and rounded to 5 g. */
function gramsForKcal(food: FoodItem, kcalTarget: number): number {
  if (food.kcal <= 0) return food.typicalServingG;
  const grams = (kcalTarget / food.kcal) * 100;
  const clamped = Math.min(MAX_GRAMS, Math.max(MIN_GRAMS, grams));
  return Math.round(clamped / 5) * 5;
}

function toItem(food: FoodItem, grams: number): MealItem {
  const factor = grams / 100;
  return {
    foodId: food.id,
    name: food.name,
    grams,
    kcal: round(food.kcal * factor, 0),
    proteinG: round(food.proteinG * factor, 1),
    carbG: round(food.carbG * factor, 1),
    fatG: round(food.fatG * factor, 1),
    fiberG: round(food.fiberG * factor, 1),
    sugarG: round(food.sugarG * factor, 1),
    sodiumMg: round(food.sodiumMg * factor, 0),
  };
}

/** Candidate foods for a slot, ordered so rotation yields variety and good protein. */
function candidatesForSlot(
  foods: FoodItem[],
  slot: MealSlot,
  dietType: string,
): FoodItem[] {
  const inSlot = foods.filter((f) => f.mealSlots.includes(slot));
  const list = inSlot.length > 0 ? inSlot : foods; // fall back to any eligible food
  const sorted = [...list];
  if (dietType === 'keto' || dietType === 'lowcarb') {
    sorted.sort((a, b) => a.carbG - b.carbG);
  } else {
    sorted.sort((a, b) => proteinDensity(b) - proteinDensity(a));
  }
  return sorted;
}

function pickRotated<T>(list: T[], offset: number): T | undefined {
  if (list.length === 0) return undefined;
  return list[((offset % list.length) + list.length) % list.length];
}

function buildMeal(
  slot: MealSlot,
  eligible: FoodItem[],
  slotKcal: number,
  dietType: string,
  dayIndex: number,
): Meal {
  const candidates = candidatesForSlot(eligible, slot, dietType);
  const items: MealItem[] = [];

  // Main meals get two complementary items; light slots get one.
  const twoItems = MAIN_SLOTS.includes(slot) && candidates.length > 1;
  const slotSeed = MEAL_SLOTS.indexOf(slot) * 3 + dayIndex * 2;

  if (twoItems) {
    const protein = pickRotated(candidates, slotSeed);
    // Second pick favours fibre/variety: rotate through a fibre-sorted list.
    const byFiber = [...candidates].sort((a, b) => b.fiberG - a.fiberG);
    let second = pickRotated(byFiber, slotSeed + 1);
    if (second && protein && second.id === protein.id) {
      second = pickRotated(byFiber, slotSeed + 2);
    }
    if (protein) items.push(toItem(protein, gramsForKcal(protein, slotKcal * 0.5)));
    if (second) items.push(toItem(second, gramsForKcal(second, slotKcal * 0.5)));
  } else {
    const only = pickRotated(candidates, slotSeed);
    if (only) items.push(toItem(only, gramsForKcal(only, slotKcal)));
  }

  const kcal = round(
    items.reduce((s, i) => s + i.kcal, 0),
    0,
  );
  const proteinG = round(
    items.reduce((s, i) => s + i.proteinG, 0),
    1,
  );
  return { slot, items, kcal, proteinG };
}

function buildDay(
  dayIndex: number,
  eligible: FoodItem[],
  targets: PlanTargets,
  dietType: string,
): DayPlan {
  // Intermittent fasting collapses to fewer eating windows.
  const slots =
    dietType === 'if'
      ? (['lunch', 'eveningsnack', 'dinner'] as MealSlot[])
      : MEAL_SLOTS;

  // Renormalise slot weights over the active slots so kcal still sums to the target.
  const weightSum = slots.reduce((s, sl) => s + SLOT_KCAL_WEIGHTS[sl], 0);

  const meals = slots.map((slot) => {
    const slotKcal = (targets.dailyKcal * SLOT_KCAL_WEIGHTS[slot]) / weightSum;
    return buildMeal(slot, eligible, slotKcal, dietType, dayIndex);
  });

  const sum = (pick: (i: MealItem) => number) =>
    meals.reduce((s, m) => s + m.items.reduce((t, i) => t + pick(i), 0), 0);

  return {
    dayIndex,
    meals,
    totals: {
      kcal: round(sum((i) => i.kcal), 0),
      proteinG: round(sum((i) => i.proteinG), 1),
      carbG: round(sum((i) => i.carbG), 1),
      fatG: round(sum((i) => i.fatG), 1),
      fiberG: round(sum((i) => i.fiberG), 1),
      sodiumMg: round(sum((i) => i.sodiumMg), 0),
    },
  };
}

export interface GenerateOptions {
  days?: number;
  /** If given, each day gets a real date + label (Today/Tomorrow/weekday). */
  startDate?: Date;
}

const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

/**
 * Deterministic `rules` meal-plan generator. Given targets, the food DB and preferences,
 * it produces a rotating multi-day plan honouring diet type, allergies and conditions.
 * No LLM, no randomness — the same inputs always yield the same plan.
 */
export function generateWeekPlan(
  foods: FoodItem[],
  targets: PlanTargets,
  prefs: PlanPreferences,
  options: GenerateOptions = {},
): WeekPlan {
  const eligible = eligibleFoods(foods, prefs);
  if (eligible.length === 0) {
    throw new Error('No foods match the diet/allergy/condition constraints');
  }
  // Bias toward the requested locale, but keep the rest as fallback.
  const ordered = prefs.locale
    ? [
        ...eligible.filter((f) => f.locale === prefs.locale),
        ...eligible.filter((f) => f.locale !== prefs.locale),
      ]
    : eligible;

  const dayCount = options.days ?? 7;
  const days: DayPlan[] = [];
  for (let d = 0; d < dayCount; d++) {
    const day = buildDay(d, ordered, targets, prefs.dietType);
    if (options.startDate) {
      const dt = new Date(options.startDate.getTime() + d * 86_400_000);
      day.date = dt.toISOString().slice(0, 10);
      day.label = d === 0 ? 'Today' : d === 1 ? 'Tomorrow' : WEEKDAYS[dt.getUTCDay()];
    }
    days.push(day);
  }
  return { days, targets };
}
