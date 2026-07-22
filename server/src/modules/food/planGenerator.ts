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

const isGrain = (f: FoodItem) => f.tags.includes('grain');
const isProteinFood = (f: FoodItem) =>
  f.tags.includes('legume') ||
  f.tags.includes('high-protein') ||
  f.category === 'egg' ||
  f.category === 'nonveg' ||
  f.tags.includes('dairy');
const isVegFood = (f: FoodItem) => f.tags.includes('vegetable') || f.tags.includes('salad');

function meal(slot: MealSlot, items: MealItem[]): Meal {
  return {
    slot,
    items,
    kcal: round(items.reduce((s, i) => s + i.kcal, 0), 0),
    proteinG: round(items.reduce((s, i) => s + i.proteinG, 0), 1),
  };
}

function buildMeal(
  slot: MealSlot,
  eligible: FoodItem[],
  slotKcal: number,
  dietType: string,
  dayIndex: number,
): Meal {
  const candidates = candidatesForSlot(eligible, slot, dietType);
  const slotSeed = MEAL_SLOTS.indexOf(slot) * 3 + dayIndex * 2;

  // Main meals = a proper plate: one staple (grain: roti OR rice, not both) + a dal/protein +
  // a vegetable when available. Light slots stay a single item.
  if (MAIN_SLOTS.includes(slot) && candidates.length > 1) {
    const grains = candidates.filter(isGrain);
    const proteins = candidates.filter((f) => isProteinFood(f) && !isGrain(f));
    const veggies = candidates.filter((f) => isVegFood(f) && !isGrain(f) && !isProteinFood(f));

    const chosen = new Set<string>();
    const items: MealItem[] = [];
    const add = (f: FoodItem | undefined, share: number) => {
      if (f && !chosen.has(f.id)) {
        chosen.add(f.id);
        items.push(toItem(f, gramsForKcal(f, slotKcal * share)));
      }
    };
    const pickNot = (list: FoodItem[], seed: number) => {
      const pool = list.filter((f) => !chosen.has(f.id));
      return pickRotated(pool.length ? pool : list, seed);
    };

    const grain = pickNot(grains.length ? grains : candidates, slotSeed);
    add(grain, veggies.length && proteins.length ? 0.45 : 0.55);
    const protein = pickNot(proteins.length ? proteins : candidates, slotSeed + 1);
    add(protein, veggies.length ? 0.35 : 0.45);
    if (veggies.length) add(pickNot(veggies, slotSeed + 2), 0.2);

    // Safety net: if we somehow only got one item, add a complementary second.
    if (items.length < 2) add(pickNot(candidates, slotSeed + 3), 0.5);
    return meal(slot, items);
  }

  const only = pickRotated(candidates, slotSeed);
  return meal(slot, only ? [toItem(only, gramsForKcal(only, slotKcal))] : []);
}

/**
 * Builds a single replacement meal for a slot at (roughly) a target calorie level, avoiding
 * the foods currently used so a "swap" actually changes the dish. `variant` rotates the pick.
 */
export function buildSwapMeal(
  slot: MealSlot,
  eligible: FoodItem[],
  kcalTarget: number,
  dietType: string,
  dayIndex: number,
  avoidIds: string[],
  variant: number,
): Meal {
  const all = candidatesForSlot(eligible, slot, dietType);
  const filtered = all.filter((f) => !avoidIds.includes(f.id));
  const pool = filtered.length > 0 ? filtered : all;
  const items: MealItem[] = [];
  const twoItems = MAIN_SLOTS.includes(slot) && pool.length > 1;
  const slotSeed = MEAL_SLOTS.indexOf(slot) * 3 + dayIndex * 2 + variant;

  if (twoItems) {
    const protein = pickRotated(pool, slotSeed);
    const byFiber = [...pool].sort((a, b) => b.fiberG - a.fiberG);
    let second = pickRotated(byFiber, slotSeed + 1);
    if (second && protein && second.id === protein.id) second = pickRotated(byFiber, slotSeed + 2);
    if (protein) items.push(toItem(protein, gramsForKcal(protein, kcalTarget * 0.5)));
    if (second) items.push(toItem(second, gramsForKcal(second, kcalTarget * 0.5)));
  } else {
    const only = pickRotated(pool, slotSeed);
    if (only) items.push(toItem(only, gramsForKcal(only, kcalTarget)));
  }

  const kcal = round(items.reduce((s, i) => s + i.kcal, 0), 0);
  const proteinG = round(items.reduce((s, i) => s + i.proteinG, 0), 1);
  return { slot, items, kcal, proteinG };
}

const LIGHT_TAGS = new Set(['fruit', 'beverage', 'light', 'probiotic', 'high-fiber']);

function buildDay(
  dayIndex: number,
  eligible: FoodItem[],
  targets: PlanTargets,
  dietType: string,
  fasting = false,
): DayPlan {
  // Fasting day: fewer, lighter meals at ~40% of calories.
  const slots: MealSlot[] = fasting
    ? (['midmorning', 'lunch', 'eveningsnack'] as MealSlot[])
    : dietType === 'if'
      ? (['lunch', 'eveningsnack', 'dinner'] as MealSlot[])
      : MEAL_SLOTS;

  const dailyKcal = fasting ? Math.round(targets.dailyKcal * 0.4) : targets.dailyKcal;

  // On a fasting day, prefer light foods (fruit, buttermilk, coconut water, khichdi).
  const pool = fasting
    ? (() => {
        const light = eligible.filter(
          (f) => f.kcal < 130 || f.tags.some((t) => LIGHT_TAGS.has(t)),
        );
        return light.length >= 3 ? light : eligible;
      })()
    : eligible;

  // Renormalise slot weights over the active slots so kcal still sums to the target.
  const weightSum = slots.reduce((s, sl) => s + SLOT_KCAL_WEIGHTS[sl], 0);

  const meals = slots.map((slot) => {
    const slotKcal = (dailyKcal * SLOT_KCAL_WEIGHTS[slot]) / weightSum;
    return buildMeal(slot, pool, slotKcal, dietType, dayIndex);
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
  /** If given, each day gets a real date + label (Yesterday/Today/Tomorrow/weekday). */
  startDate?: Date;
  /** Reference "today" for labels (defaults to startDate). */
  today?: Date;
  /** Optional weekly fasting day: 0=Sun .. 6=Sat. That day gets a light plan. */
  fastDayOfWeek?: number;
}

/** Label a date relative to today: Yesterday / Today / Tomorrow / weekday name. */
export function dayLabel(date: Date, today: Date): string {
  const a = Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
  const b = Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate());
  const diff = Math.round((a - b) / 86_400_000);
  if (diff === 0) return 'Today';
  if (diff === 1) return 'Tomorrow';
  if (diff === -1) return 'Yesterday';
  return WEEKDAYS[date.getUTCDay()]!;
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
    let fasting = false;
    let dt: Date | undefined;
    if (options.startDate) {
      dt = new Date(options.startDate.getTime() + d * 86_400_000);
      fasting = options.fastDayOfWeek !== undefined && dt.getUTCDay() === options.fastDayOfWeek;
    }
    const day = buildDay(d, ordered, targets, prefs.dietType, fasting);
    if (dt) {
      day.date = dt.toISOString().slice(0, 10);
      const base = dayLabel(dt, options.today ?? options.startDate ?? dt);
      day.label = fasting ? `${base} · Fasting day` : base;
    }
    days.push(day);
  }
  return { days, targets };
}
