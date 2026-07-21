import {
  Anthropometrics,
  BMI_HEALTHY_MAX,
  BMI_HEALTHY_MIN,
  BmiResult,
  Sex,
} from './types';

/** Rounds to `dp` decimal places (banker-free, standard round-half-up). */
export function round(value: number, dp = 1): number {
  const f = 10 ** dp;
  return Math.round(value * f) / f;
}

/** Body Mass Index (kg/m²). */
export function bmi(weightKg: number, heightCm: number): number {
  if (heightCm <= 0) throw new RangeError('heightCm must be > 0');
  if (weightKg <= 0) throw new RangeError('weightKg must be > 0');
  const m = heightCm / 100;
  return round(weightKg / (m * m), 1);
}

export function bmiCategory(bmiValue: number): BmiResult['category'] {
  if (bmiValue < 18.5) return 'underweight';
  if (bmiValue < 25) return 'normal';
  if (bmiValue < 30) return 'overweight';
  return 'obese';
}

export function bmiResult(weightKg: number, heightCm: number): BmiResult {
  const value = bmi(weightKg, heightCm);
  return { bmi: value, category: bmiCategory(value) };
}

/** Waist-to-height ratio. Undefined when waist is not provided. */
export function whtr(waistCm: number | undefined, heightCm: number): number | undefined {
  if (waistCm === undefined) return undefined;
  if (heightCm <= 0) throw new RangeError('heightCm must be > 0');
  return round(waistCm / heightCm, 2);
}

/** Healthy weight range (kg) for the given height, from the healthy BMI band. */
export function idealWeightRange(heightCm: number): { minKg: number; maxKg: number } {
  if (heightCm <= 0) throw new RangeError('heightCm must be > 0');
  const m = heightCm / 100;
  return {
    minKg: round(BMI_HEALTHY_MIN * m * m, 1),
    maxKg: round(BMI_HEALTHY_MAX * m * m, 1),
  };
}

/**
 * Body-fat % estimate via the Deurenberg equation:
 *   BF% = 1.20*BMI + 0.23*age − 10.8*sexFactor − 5.4   (male=1, female=0)
 * A rough fallback when no measured body composition is available; clamped to [3, 65].
 */
export function bodyFatDeurenberg(params: {
  bmi: number;
  ageYears: number;
  sex: Sex;
}): number {
  const sexFactor = params.sex === 'male' ? 1 : 0;
  const bf = 1.2 * params.bmi + 0.23 * params.ageYears - 10.8 * sexFactor - 5.4;
  return round(Math.min(65, Math.max(3, bf)), 1);
}

/** Convenience: compute all anthropometric metrics at once. */
export function anthropometrySummary(a: Anthropometrics) {
  const b = bmiResult(a.weightKg, a.heightCm);
  return {
    ...b,
    whtr: whtr(a.waistCm, a.heightCm),
    idealWeight: idealWeightRange(a.heightCm),
    bodyFatEstimate: bodyFatDeurenberg({
      bmi: b.bmi,
      ageYears: a.ageYears,
      sex: a.sex,
    }),
  };
}
