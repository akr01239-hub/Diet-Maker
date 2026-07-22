import { prisma } from '../../lib/prisma';
import { HttpError } from '../../middleware/error';
import { requireCompleteProfile } from '../profile/profile.service';
import { computeCalcResult } from '../nutrition/calcResult';
import { ageFromDob } from '../nutrition/calc.service';
import { generateWeekPlan, buildSwapMeal } from './planGenerator';
import { eligibleFoods } from './foodFilter';
import { localSunday, localToday } from '../../lib/tz';
import { round } from '../../calc/anthropometry';
import type { DayPlan, FoodItem, MealSlot, PlanPreferences, PlanTargets } from './food.types';
import { SLOT_KCAL_WEIGHTS } from './food.types';
import type { ActivityLevel, Goal } from '../../calc/types';
import type { Condition } from '../../guardrails';
import type { Food } from '@prisma/client';

function toFoodItem(f: Food): FoodItem {
  return {
    id: f.id,
    name: f.name,
    locale: f.locale,
    region: f.region ?? undefined,
    category: f.category as FoodItem['category'],
    mealSlots: f.mealSlots as MealSlot[],
    kcal: f.kcal,
    proteinG: f.proteinG,
    carbG: f.carbG,
    fatG: f.fatG,
    fiberG: f.fiberG,
    sugarG: f.sugarG,
    sodiumMg: f.sodiumMg,
    glycemicIndex: f.glycemicIndex ?? undefined,
    typicalServingG: f.typicalServingG,
    costTier: (f.costTier as 1 | 2 | 3) ?? 2,
    tags: f.tags,
    allergens: f.allergens,
  };
}

/**
 * Builds a fresh plan for a user: compute deterministic targets, run the rules generator
 * through diet/allergy/condition filters, persist and return. Every plan is guardrail-safe
 * because its calorie/macro targets come from the guardrailed CalcResult.
 */
export async function generateAndSavePlan(
  userId: string,
  days = 7,
  tzOffsetMin = 0,
  kcalDeltaOverride = 0,
) {
  const { profile, sensitive } = await requireCompleteProfile(userId);

  const calc = computeCalcResult({
    heightCm: profile.heightCm,
    currentWeightKg: sensitive.currentWeightKg,
    targetWeightKg: sensitive.targetWeightKg,
    ageYears: ageFromDob(sensitive.dob),
    sex: sensitive.sex,
    activityLevel: profile.activityLevel as ActivityLevel,
    goal: profile.goal as Goal,
    waistCm: sensitive.waistCm,
    conditions: sensitive.conditions as Condition[],
    desiredWeeklyLossKg: sensitive.desiredWeeklyLossKg,
    clinicianOverride: sensitive.clinicianOverride,
    reducedMobility: profile.reducedMobility,
  });

  const targets: PlanTargets = {
    dailyKcal: calc.dailyKcal,
    proteinG: calc.proteinG,
    fatG: calc.fatG,
    carbG: calc.carbG,
    fiberG: calc.fiberG,
    sodiumMaxMg: calc.sodiumMaxMg,
  };

  const prefs: PlanPreferences = {
    dietType: profile.dietType,
    allergies: sensitive.allergies,
    conditions: sensitive.conditions,
    locale: 'IN',
  };

  const foods = await prisma.food.findMany();
  if (foods.length === 0) {
    throw new HttpError(503, 'Food database is empty — run the seed');
  }

  // ---- Calorie carry-over: compensate next week for the last 7 days' surplus/deficit ----
  const now = new Date();
  const since = new Date(now.getTime() - 7 * 86_400_000);
  const recentLogs = await prisma.foodLog.findMany({
    where: { userId, loggedAt: { gte: since } },
    select: { loggedAt: true, kcal: true },
  });
  const perDay = new Map<string, number>();
  for (const l of recentLogs) {
    const k = l.loggedAt.toISOString().slice(0, 10);
    perDay.set(k, (perDay.get(k) ?? 0) + l.kcal);
  }
  let cumulativeDelta = 0; // + = ate over target across logged days
  for (const kcal of perDay.values()) cumulativeDelta += kcal - calc.dailyKcal;
  // Spread the surplus/deficit over the coming week, capped ±300/day for safety.
  const carryOverKcal = Math.max(-300, Math.min(300, Math.round(-cumulativeDelta / 7)));
  targets.dailyKcal = Math.max(1200, calc.dailyKcal + carryOverKcal);
  targets.carryOverKcal = carryOverKcal;

  // Adaptive override: when the user applies the coach's recommendation, bake the
  // suggested calorie delta into this plan (still floored for safety).
  if (kcalDeltaOverride) {
    targets.dailyKcal = Math.max(1200, targets.dailyKcal + kcalDeltaOverride);
  }

  // Sunday-to-Saturday week in the user's timezone (falls back to UTC when offset is 0).
  const week = generateWeekPlan(foods.map(toFoodItem), targets, prefs, {
    days,
    startDate: localSunday(tzOffsetMin),
    today: localToday(tzOffsetMin),
    fastDayOfWeek: sensitive.fastDayOfWeek,
  });

  const saved = await prisma.dietPlan.create({
    data: {
      userId,
      days: week.days as unknown as object,
      targets: week.targets as unknown as object,
    },
  });
  await prisma.auditLog.create({
    data: { userId, action: 'plan.generate', detail: `${days}-day plan` },
  });

  return { id: saved.id, createdAt: saved.createdAt, ...week, flags: calc.flags };
}

/**
 * Swaps a single meal in the latest plan for a different dish at (roughly) the same calories,
 * so the user can replace something they don't like without regenerating the whole week.
 */
export async function swapMeal(userId: string, dayIndex: number, slot: MealSlot) {
  const planRow = await prisma.dietPlan.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  if (!planRow) throw new HttpError(404, 'No plan yet — generate one first');

  const days = planRow.days as unknown as DayPlan[];
  const targets = planRow.targets as unknown as PlanTargets;
  const day = days.find((d) => d.dayIndex === dayIndex) ?? days[dayIndex];
  if (!day) throw new HttpError(400, 'Invalid day');
  const mealIdx = day.meals.findIndex((m) => m.slot === slot);
  if (mealIdx < 0) throw new HttpError(400, 'No such meal on this day');

  const current = day.meals[mealIdx]!;
  const kcalTarget =
    current.kcal > 0 ? current.kcal : Math.round(targets.dailyKcal * (SLOT_KCAL_WEIGHTS[slot] ?? 0.2));

  const { profile, sensitive } = await requireCompleteProfile(userId);
  const foods = await prisma.food.findMany();
  const prefs: PlanPreferences = {
    dietType: profile.dietType,
    allergies: sensitive.allergies,
    conditions: sensitive.conditions,
    locale: 'IN',
  };
  const eligible = eligibleFoods(foods.map(toFoodItem), prefs);
  const ordered = [
    ...eligible.filter((f) => f.locale === prefs.locale),
    ...eligible.filter((f) => f.locale !== prefs.locale),
  ];

  const avoidIds = current.items.map((i) => i.foodId);
  const newMeal = buildSwapMeal(slot, ordered, kcalTarget, prefs.dietType, dayIndex, avoidIds, 1);
  day.meals[mealIdx] = newMeal;

  // Recompute the day totals from its meals.
  const sum = (pick: (i: (typeof newMeal.items)[number]) => number) =>
    day.meals.reduce((s, m) => s + m.items.reduce((t, i) => t + pick(i), 0), 0);
  day.totals = {
    kcal: round(sum((i) => i.kcal), 0),
    proteinG: round(sum((i) => i.proteinG), 1),
    carbG: round(sum((i) => i.carbG), 1),
    fatG: round(sum((i) => i.fatG), 1),
    fiberG: round(sum((i) => i.fiberG), 1),
    sodiumMg: round(sum((i) => i.sodiumMg), 0),
  };

  await prisma.dietPlan.update({
    where: { id: planRow.id },
    data: { days: days as unknown as object },
  });

  return { id: planRow.id, createdAt: planRow.createdAt, days, targets };
}

export async function latestPlan(userId: string) {
  const plan = await prisma.dietPlan.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  if (!plan) return null;
  return { id: plan.id, createdAt: plan.createdAt, days: plan.days, targets: plan.targets };
}
