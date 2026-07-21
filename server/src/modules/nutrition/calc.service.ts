import { prisma } from '../../lib/prisma';
import { requireCompleteProfile } from '../profile/profile.service';
import { computeCalcResult, type CalcResult } from './calcResult';
import type { ActivityLevel, Goal } from '../../calc/types';
import type { Condition } from '../../guardrails';

/** Whole years between dob and now. */
export function ageFromDob(dobISO: string, now: Date = new Date()): number {
  const dob = new Date(dobISO);
  let age = now.getUTCFullYear() - dob.getUTCFullYear();
  const m = now.getUTCMonth() - dob.getUTCMonth();
  if (m < 0 || (m === 0 && now.getUTCDate() < dob.getUTCDate())) age -= 1;
  return age;
}

/** Computes the authoritative CalcResult for a user and persists a versioned snapshot. */
export async function computeAndSaveForUser(userId: string): Promise<CalcResult> {
  const { profile, sensitive } = await requireCompleteProfile(userId);

  const result = computeCalcResult({
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

  await prisma.calcResultSnapshot.create({
    data: { userId, result: result as unknown as object },
  });
  await prisma.auditLog.create({
    data: { userId, action: 'calc.compute', detail: 'CalcResult snapshot saved' },
  });
  return result;
}

export async function latestCalcResult(userId: string) {
  const snap = await prisma.calcResultSnapshot.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  return snap?.result ?? null;
}
