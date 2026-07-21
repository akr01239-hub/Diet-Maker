import {
  ActivityLevel,
  Goal,
  Sex,
  anthropometrySummary,
  computeMacros,
  energy,
} from '../../calc';
import { applyGuardrails, Condition, Flag } from '../../guardrails';

/** Everything the /calc endpoint needs, in one call. */
export interface CalcProfileInput {
  heightCm: number;
  currentWeightKg: number;
  targetWeightKg: number;
  ageYears: number;
  sex: Sex;
  activityLevel: ActivityLevel;
  goal: Goal;
  waistCm?: number;
  conditions?: Condition[];
  desiredWeeklyLossKg?: number;
  clinicianOverride?: boolean;
  reducedMobility?: boolean;
}

export interface CalcResult {
  bmi: number;
  bmiCategory: string;
  whtr?: number;
  bmr: number;
  tdee: number;
  idealWeightMinKg: number;
  idealWeightMaxKg: number;
  bodyFatEstimate: number;
  dailyKcal: number;
  proteinG: number;
  fatG: number;
  carbG: number;
  fiberG: number;
  waterMl: number;
  sodiumMaxMg?: number;
  safeWeeklyDeltaKg: number;
  requiresSupervision: boolean;
  weightLossBlocked: boolean;
  flags: Flag[];
}

/**
 * Runs the deterministic pipeline: anthropometry -> energy -> guardrails -> macros.
 * This is the authoritative snapshot persisted as a versioned CalcResult.
 */
export function computeCalcResult(input: CalcProfileInput): CalcResult {
  const conditions = input.conditions ?? [];

  const anthro = anthropometrySummary({
    heightCm: input.heightCm,
    weightKg: input.currentWeightKg,
    ageYears: input.ageYears,
    sex: input.sex,
    waistCm: input.waistCm,
  });

  const { bmr, tdee } = energy(
    {
      heightCm: input.heightCm,
      weightKg: input.currentWeightKg,
      ageYears: input.ageYears,
      sex: input.sex,
    },
    input.activityLevel,
  );

  const guard = applyGuardrails({
    sex: input.sex,
    ageYears: input.ageYears,
    currentWeightKg: input.currentWeightKg,
    targetWeightKg: input.targetWeightKg,
    tdee,
    bmi: anthro.bmi,
    goal: input.goal,
    conditions,
    desiredWeeklyLossKg: input.desiredWeeklyLossKg,
    clinicianOverride: input.clinicianOverride,
    reducedMobility: input.reducedMobility,
  });

  const macros = computeMacros({
    dailyKcal: guard.dailyKcal,
    targetWeightKg: input.targetWeightKg,
    currentWeightKg: input.currentWeightKg,
    proteinPerKg: guard.proteinPerKg,
    fatPerKgMin: guard.fatPerKgMin,
  });

  return {
    bmi: anthro.bmi,
    bmiCategory: anthro.category,
    whtr: anthro.whtr,
    bmr,
    tdee,
    idealWeightMinKg: anthro.idealWeight.minKg,
    idealWeightMaxKg: anthro.idealWeight.maxKg,
    bodyFatEstimate: anthro.bodyFatEstimate,
    dailyKcal: guard.dailyKcal,
    proteinG: macros.proteinG,
    fatG: macros.fatG,
    carbG: macros.carbG,
    fiberG: macros.fiberG,
    waterMl: macros.waterMl,
    sodiumMaxMg: guard.sodiumMaxMg,
    safeWeeklyDeltaKg: guard.safeWeeklyDeltaKg,
    requiresSupervision: guard.requiresSupervision,
    weightLossBlocked: guard.weightLossBlocked,
    flags: guard.flags,
  };
}
