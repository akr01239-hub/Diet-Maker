import { prisma } from '../../lib/prisma';
import { HttpError } from '../../middleware/error';
import { requireCompleteProfile } from '../profile/profile.service';
import { computeCalcResult } from '../nutrition/calcResult';
import { ageFromDob } from '../nutrition/calc.service';
import { generateWeekPlan } from './planGenerator';
import { localSunday, localToday } from '../../lib/tz';
import type { FoodItem, MealSlot, PlanPreferences, PlanTargets } from './food.types';
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
export async function generateAndSavePlan(userId: string, days = 7, tzOffsetMin = 0) {
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

export async function latestPlan(userId: string) {
  const plan = await prisma.dietPlan.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  if (!plan) return null;
  return { id: plan.id, createdAt: plan.createdAt, days: plan.days, targets: plan.targets };
}
