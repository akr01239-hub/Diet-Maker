import { prisma } from '../../lib/prisma';
import { decryptJson, encryptJson } from '../../lib/crypto';
import { round } from '../../calc/anthropometry';
import { HttpError } from '../../middleware/error';
import type { CheckinBody, FoodLogBody, WaterLogBody } from './logging.schemas';
import { buildDashboard, dayKey, sumTotals, type WeightPoint } from './dashboard';
import { localDayKey } from '../../lib/tz';

/**
 * UTC [start, end) instants bounding the local calendar day containing `date`.
 * offsetMin = the user's UTC offset in minutes (0 => plain UTC day, backward-compatible).
 */
export function dayRange(date: Date, offsetMin = 0): { start: Date; end: Date } {
  const w = new Date(date.getTime() + offsetMin * 60_000);
  const startMs = Date.UTC(w.getUTCFullYear(), w.getUTCMonth(), w.getUTCDate()) - offsetMin * 60_000;
  return { start: new Date(startMs), end: new Date(startMs + 86_400_000) };
}

async function nutritionBasis(body: FoodLogBody) {
  if (body.foodId) {
    const food = await prisma.food.findUnique({ where: { id: body.foodId } });
    if (!food) throw new HttpError(404, 'Food not found');
    return {
      name: body.foodName ?? food.name,
      per100g: {
        kcal: food.kcal,
        proteinG: food.proteinG,
        carbG: food.carbG,
        fatG: food.fatG,
        fiberG: food.fiberG,
        sugarG: food.sugarG,
        sodiumMg: food.sodiumMg,
      },
    };
  }
  // Validated by schema refine: foodName + per100g present here.
  return { name: body.foodName!, per100g: body.per100g! };
}

export async function logFood(userId: string, body: FoodLogBody) {
  const basis = await nutritionBasis(body);
  const factor = body.grams / 100;
  const entry = await prisma.foodLog.create({
    data: {
      userId,
      loggedAt: body.loggedAt ? new Date(body.loggedAt) : new Date(),
      mealSlot: body.mealSlot,
      foodId: body.foodId ?? null,
      foodName: basis.name,
      grams: body.grams,
      entryMethod: body.entryMethod,
      kcal: round(basis.per100g.kcal * factor, 0),
      proteinG: round(basis.per100g.proteinG * factor, 1),
      carbG: round(basis.per100g.carbG * factor, 1),
      fatG: round(basis.per100g.fatG * factor, 1),
      fiberG: round(basis.per100g.fiberG * factor, 1),
      sugarG: round(basis.per100g.sugarG * factor, 1),
      sodiumMg: round(basis.per100g.sodiumMg * factor, 0),
      notes: body.notes ?? null,
      mood: body.mood ?? null,
      hunger: body.hunger ?? null,
    },
  });
  return entry;
}

export async function listFood(userId: string, date: Date, offsetMin = 0) {
  const { start, end } = dayRange(date, offsetMin);
  return prisma.foodLog.findMany({
    where: { userId, loggedAt: { gte: start, lt: end } },
    orderBy: { loggedAt: 'asc' },
  });
}

export async function deleteFood(userId: string, id: string) {
  const res = await prisma.foodLog.deleteMany({ where: { id, userId } });
  if (res.count === 0) throw new HttpError(404, 'Log entry not found');
}

export async function logWater(userId: string, body: WaterLogBody) {
  return prisma.waterLog.create({
    data: {
      userId,
      amountMl: body.amountMl,
      loggedAt: body.loggedAt ? new Date(body.loggedAt) : new Date(),
    },
  });
}

export async function waterTotal(userId: string, date: Date, offsetMin = 0): Promise<number> {
  const { start, end } = dayRange(date, offsetMin);
  const agg = await prisma.waterLog.aggregate({
    where: { userId, loggedAt: { gte: start, lt: end } },
    _sum: { amountMl: true },
  });
  return agg._sum.amountMl ?? 0;
}

export async function createCheckin(userId: string, body: CheckinBody) {
  const { date, energy, sleepHours, mood, pain, notes, ...measurements } = body;
  const checkin = await prisma.weeklyCheckin.create({
    data: {
      userId,
      date: date ? new Date(date) : new Date(),
      measurementsEnc: encryptJson(measurements),
      energy: energy ?? null,
      sleepHours: sleepHours ?? null,
      mood: mood ?? null,
      pain: pain ?? null,
      notes: notes ?? null,
    },
  });
  await prisma.auditLog.create({
    data: { userId, action: 'checkin.create', detail: 'weight/measurements written' },
  });
  return { id: checkin.id, date: checkin.date };
}

export async function listCheckins(userId: string) {
  const rows = await prisma.weeklyCheckin.findMany({
    where: { userId },
    orderBy: { date: 'asc' },
  });
  return rows.map((r) => {
    const m = r.measurementsEnc
      ? decryptJson<{ weightKg: number; waistCm?: number }>(r.measurementsEnc)
      : null;
    return {
      id: r.id,
      date: r.date,
      measurements: m,
      energy: r.energy,
      sleepHours: r.sleepHours,
      mood: r.mood,
      pain: r.pain,
      notes: r.notes,
    };
  });
}

export async function getDashboard(userId: string, offsetMin = 0, now: Date = new Date()) {
  const todayKey = localDayKey(offsetMin, now.getTime());

  const [todayFood, waterMl, snapshot, checkins, distinctDays] = await Promise.all([
    listFood(userId, now, offsetMin),
    waterTotal(userId, now, offsetMin),
    prisma.calcResultSnapshot.findFirst({ where: { userId }, orderBy: { createdAt: 'desc' } }),
    prisma.weeklyCheckin.findMany({ where: { userId }, orderBy: { date: 'asc' } }),
    prisma.foodLog.findMany({ where: { userId }, select: { loggedAt: true } }),
  ]);

  const result = snapshot?.result as
    | {
        dailyKcal: number;
        proteinG: number;
        waterMl: number;
        bmi: number;
        projection?: { label: string; weeks: number; weightKg: number; bmi: number }[];
      }
    | undefined;

  const weightPoints: WeightPoint[] = checkins
    .map((c) => {
      if (!c.measurementsEnc) return null;
      const m = decryptJson<{ weightKg: number }>(c.measurementsEnc);
      return { date: c.date.toISOString(), weightKg: m.weightKg };
    })
    .filter((p): p is WeightPoint => p !== null);

  return buildDashboard({
    targets: result
      ? { dailyKcal: result.dailyKcal, proteinG: result.proteinG, waterMl: result.waterMl }
      : null,
    todayTotals: sumTotals(todayFood),
    waterTodayMl: waterMl,
    logDayKeys: distinctDays.map((d) => dayKey(d.loggedAt)),
    todayKey,
    weightPoints,
    bmi: result?.bmi ?? null,
    projection: result?.projection ?? [],
  });
}
