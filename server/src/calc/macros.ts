import { MacroTargets } from './types';
import { round } from './anthropometry';

export interface MacroInput {
  /** Final daily calorie target (already safety-adjusted by guardrails). */
  dailyKcal: number;
  /** Weight used for per-kg macro targets (usually target weight). */
  targetWeightKg: number;
  /** Weight used for water target (usually current weight). */
  currentWeightKg: number;
  /** Protein g/kg (1.6–2.2 typical; guardrails may lower for CKD). */
  proteinPerKg?: number;
  /** Minimum fat g/kg (default 0.6). Fat is also floored at 20% of kcal. */
  fatPerKgMin?: number;
  /** Water ml/kg (30–35 typical). */
  waterPerKg?: number;
}

const KCAL_PER_G_PROTEIN = 4;
const KCAL_PER_G_CARB = 4;
const KCAL_PER_G_FAT = 9;

/**
 * Splits a calorie target into macros.
 * - protein: proteinPerKg × targetWeight
 * - fat: max(fatPerKgMin × targetWeight, 20% of kcal ÷ 9)
 * - carbs: remaining kcal (never negative)
 * - fiber: 14 g per 1000 kcal
 * - water: waterPerKg × currentWeight
 */
export function computeMacros(input: MacroInput): MacroTargets {
  const {
    dailyKcal,
    targetWeightKg,
    currentWeightKg,
    proteinPerKg = 1.8,
    fatPerKgMin = 0.6,
    waterPerKg = 33,
  } = input;

  if (dailyKcal <= 0) throw new RangeError('dailyKcal must be > 0');

  const proteinG = round(proteinPerKg * targetWeightKg, 0);
  const fatFromWeight = fatPerKgMin * targetWeightKg;
  const fatFromKcal = (0.2 * dailyKcal) / KCAL_PER_G_FAT;
  const fatG = round(Math.max(fatFromWeight, fatFromKcal), 0);

  const remainingKcal =
    dailyKcal - proteinG * KCAL_PER_G_PROTEIN - fatG * KCAL_PER_G_FAT;
  const carbG = round(Math.max(0, remainingKcal / KCAL_PER_G_CARB), 0);

  const fiberG = round(14 * (dailyKcal / 1000), 0);
  const waterMl = round(waterPerKg * currentWeightKg, 0);

  return { proteinG, fatG, carbG, fiberG, waterMl };
}
