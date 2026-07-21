import { prisma } from '../../lib/prisma';
import { decryptJson } from '../../lib/crypto';
import { round } from '../../calc/anthropometry';
import { HttpError } from '../../middleware/error';
import { dayKey, computeStreak, weightTrend, type WeightPoint } from '../logging/dashboard';
import { waterTotal, listFood } from '../logging/logging.service';
import type { DayPlan, FoodItem, MealSlot } from '../food/food.types';
import { buildGroceryList } from './grocery';
import { buildWeeklyReport, type ReportDay } from './report';
import { computeBadges, badgeSummary, type GamificationStats } from './gamification';

function toFoodItem(f: {
  id: string; name: string; locale: string; region: string | null; category: string;
  mealSlots: string[]; kcal: number; proteinG: number; carbG: number; fatG: number;
  fiberG: number; sugarG: number; sodiumMg: number; glycemicIndex: number | null;
  typicalServingG: number; costTier: number; tags: string[]; allergens: string[];
}): FoodItem {
  return {
    id: f.id, name: f.name, locale: f.locale, region: f.region ?? undefined,
    category: f.category as FoodItem['category'], mealSlots: f.mealSlots as MealSlot[],
    kcal: f.kcal, proteinG: f.proteinG, carbG: f.carbG, fatG: f.fatG, fiberG: f.fiberG,
    sugarG: f.sugarG, sodiumMg: f.sodiumMg, glycemicIndex: f.glycemicIndex ?? undefined,
    typicalServingG: f.typicalServingG, costTier: (f.costTier as 1 | 2 | 3) ?? 2,
    tags: f.tags, allergens: f.allergens,
  };
}

async function latestTargets(userId: string) {
  const snap = await prisma.calcResultSnapshot.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  return snap?.result as
    | { dailyKcal: number; proteinG: number; waterMl: number; bmi: number }
    | undefined;
}

async function weightPoints(userId: string): Promise<WeightPoint[]> {
  const checkins = await prisma.weeklyCheckin.findMany({ where: { userId }, orderBy: { date: 'asc' } });
  return checkins
    .map((c) => {
      if (!c.measurementsEnc) return null;
      try {
        return { date: c.date.toISOString(), weightKg: decryptJson<{ weightKg: number }>(c.measurementsEnc).weightKg };
      } catch {
        return null;
      }
    })
    .filter((p): p is WeightPoint => p !== null);
}

export async function getGrocery(userId: string) {
  const plan = await prisma.dietPlan.findFirst({ where: { userId }, orderBy: { createdAt: 'desc' } });
  if (!plan) throw new HttpError(404, 'Generate a plan first');
  const foods = await prisma.food.findMany();
  const days = plan.days as unknown as DayPlan[];
  return buildGroceryList(days, foods.map(toFoodItem));
}

async function last7Days(userId: string, now: Date): Promise<ReportDay[]> {
  const since = new Date(now.getTime() - 7 * 86_400_000);
  const logs = await prisma.foodLog.findMany({
    where: { userId, loggedAt: { gte: since } },
    select: { loggedAt: true, kcal: true, proteinG: true },
  });
  const map = new Map<string, { kcal: number; proteinG: number }>();
  for (const l of logs) {
    const k = dayKey(l.loggedAt);
    const cur = map.get(k) ?? { kcal: 0, proteinG: 0 };
    cur.kcal += l.kcal;
    cur.proteinG += l.proteinG;
    map.set(k, cur);
  }
  return [...map.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([date, v]) => ({ date, kcal: round(v.kcal, 0), proteinG: round(v.proteinG, 1) }));
}

export async function getWeeklyReport(userId: string, generatedAt: string, now: Date = new Date()) {
  const [user, targets, wp, days] = await Promise.all([
    prisma.user.findUnique({ where: { id: userId } }),
    latestTargets(userId),
    weightPoints(userId),
    last7Days(userId, now),
  ]);
  const trend = weightTrend(wp);
  return buildWeeklyReport({
    name: user ? `${user.firstName} ${user.lastName}` : 'NutriAI user',
    generatedAt,
    targets: targets ? { dailyKcal: targets.dailyKcal, proteinG: targets.proteinG, waterMl: targets.waterMl } : null,
    bmi: targets?.bmi ?? null,
    latestWeightKg: trend.latestKg,
    weightDeltaKg: trend.deltaKg,
    days,
  });
}

export async function getGamification(userId: string, now: Date = new Date()) {
  const targets = await latestTargets(userId);
  const [totalFoodLogs, todayFood, waterMl, wp, checkinCount, distinctDays] = await Promise.all([
    prisma.foodLog.count({ where: { userId } }),
    listFood(userId, now),
    waterTotal(userId, now),
    weightPoints(userId),
    prisma.weeklyCheckin.count({ where: { userId } }),
    prisma.foodLog.findMany({ where: { userId }, select: { loggedAt: true } }),
  ]);

  const todayProtein = todayFood.reduce((s, e) => s + e.proteinG, 0);
  const trend = weightTrend(wp);
  const weightLostKg = trend.firstKg !== null && trend.latestKg !== null ? round(trend.firstKg - trend.latestKg, 1) : 0;

  const stats: GamificationStats = {
    totalFoodLogs,
    streakDays: computeStreak(distinctDays.map((d) => dayKey(d.loggedAt)), dayKey(now)),
    metWaterTargetToday: targets ? waterMl >= targets.waterMl : false,
    metProteinTargetToday: targets ? todayProtein >= targets.proteinG : false,
    weightLostKg,
    checkinCount,
  };
  return badgeSummary(computeBadges(stats));
}
