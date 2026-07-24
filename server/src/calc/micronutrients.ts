import type { Sex } from './types';
import { round } from './anthropometry';

/**
 * Micronutrient RDA targets and intake-vs-RDA assessment. Deterministic, pure.
 *
 * RDA values are adult reference intakes from the US National Academies (DRI) / ICMR-NIN 2020
 * for the Indian population where they differ; chosen conservatively. Units:
 *   iron mg, calcium mg, vitaminB12 mcg, vitaminD mcg, folate mcg (DFE), potassium mg, magnesium mg.
 * These are educational targets, not a prescription — deficiency needs a blood test to confirm.
 */
export interface Micronutrients {
  ironMg: number;
  calciumMg: number;
  vitaminB12Mcg: number;
  vitaminDMcg: number;
  folateMcg: number;
  potassiumMg: number;
  magnesiumMg: number;
}

export type MicronutrientKey = keyof Micronutrients;

export const MICRONUTRIENT_LABELS: Record<MicronutrientKey, string> = {
  ironMg: 'Iron',
  calciumMg: 'Calcium',
  vitaminB12Mcg: 'Vitamin B12',
  vitaminDMcg: 'Vitamin D',
  folateMcg: 'Folate',
  potassiumMg: 'Potassium',
  magnesiumMg: 'Magnesium',
};

/** RDA for the given sex/age (adult defaults; sex- and age-adjusted where it matters). */
export function micronutrientTargets(sex: Sex, ageYears: number): Micronutrients {
  const female = sex === 'female';
  const older = ageYears >= 51;
  return {
    // Iron: pre-menopausal women need much more (menstrual losses).
    ironMg: female && ageYears < 51 ? 18 : 8,
    // Calcium rises after 50 (esp. women, bone loss).
    calciumMg: older ? 1200 : 1000,
    vitaminB12Mcg: 2.4,
    // Vitamin D: 15 mcg (600 IU) adults, 20 mcg (800 IU) for 70+.
    vitaminDMcg: ageYears >= 70 ? 20 : 15,
    folateMcg: 400,
    // Potassium (adequate intake): men higher than women.
    potassiumMg: female ? 2600 : 3400,
    // Magnesium: men higher; slight bump after 30.
    magnesiumMg: female ? (ageYears >= 31 ? 320 : 310) : ageYears >= 31 ? 420 : 400,
  };
}

export interface MicronutrientStatus {
  key: MicronutrientKey;
  label: string;
  intake: number;
  rda: number;
  /** Intake as a % of RDA (0..999, capped for display). */
  pct: number;
  /** True when clearly below adequacy (< 67% of RDA). */
  low: boolean;
}

export interface MicronutrientAssessment {
  nutrients: MicronutrientStatus[];
  /** Average coverage across all tracked micros, 0..100 (each capped at 100). */
  coveragePct: number;
  /** Keys flagged low, worst first. */
  deficiencies: MicronutrientKey[];
}

const LOW_THRESHOLD = 0.67; // < 67% of RDA reads as a real gap

/**
 * Compares a day's micronutrient intake against RDA. Missing intake is treated as 0 (not logged),
 * which naturally surfaces as "low" — but callers should only trust this once enough food is logged.
 */
export function assessMicronutrients(
  intake: Partial<Micronutrients>,
  sex: Sex,
  ageYears: number,
): MicronutrientAssessment {
  const rda = micronutrientTargets(sex, ageYears);
  const keys = Object.keys(rda) as MicronutrientKey[];
  const nutrients: MicronutrientStatus[] = keys.map((key) => {
    const got = intake[key] ?? 0;
    const target = rda[key];
    const ratio = target > 0 ? got / target : 0;
    return {
      key,
      label: MICRONUTRIENT_LABELS[key],
      intake: round(got, 1),
      rda: round(target, 1),
      pct: Math.min(999, Math.round(ratio * 100)),
      low: ratio < LOW_THRESHOLD,
    };
  });
  const coveragePct = round(
    nutrients.reduce((s, n) => s + Math.min(100, n.pct), 0) / nutrients.length,
    0,
  );
  const deficiencies = nutrients
    .filter((n) => n.low)
    .sort((a, b) => a.pct - b.pct)
    .map((n) => n.key);
  return { nutrients, coveragePct, deficiencies };
}
