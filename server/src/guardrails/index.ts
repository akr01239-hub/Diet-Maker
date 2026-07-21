import { round } from '../calc/anthropometry';
import { Condition, Flag, GuardrailInput, GuardrailResult } from './types';

export * from './types';

/** Approx. energy in 1 kg of body fat. */
export const KCAL_PER_KG_FAT = 7700;
/** Max safe weekly loss as a fraction of current body weight. */
export const MAX_WEEKLY_LOSS_FRACTION = 0.0075;
/** Max deficit as a fraction of TDEE. */
export const MAX_DEFICIT_PCT = 0.25;
/** Calorie floors below which we won't cut unless a clinician authorises it. */
export const CALORIE_FLOOR = { female: 1200, male: 1500 } as const;

/** Extra calories for pregnancy / breastfeeding (simplified, 2nd–3rd trimester style). */
const PREGNANCY_KCAL = 340;
const BREASTFEEDING_KCAL = 450;

const DISCLAIMER: Flag = {
  code: 'DISCLAIMER',
  severity: 'info',
  message: 'Educational guidance, not medical advice — consult a professional.',
};

function has(conditions: Condition[], c: Condition): boolean {
  return conditions.includes(c);
}

/**
 * The single safety gate. Takes raw goal + TDEE + conditions and returns a
 * safety-adjusted calorie/macro target plus the flags that must be surfaced.
 * Deterministic and fully unit-tested — never delegated to an LLM.
 */
export function applyGuardrails(input: GuardrailInput): GuardrailResult {
  const {
    sex,
    ageYears,
    currentWeightKg,
    tdee,
    bmi,
    goal,
    conditions,
    clinicianOverride = false,
    reducedMobility = false,
  } = input;

  const isMinor = input.isMinor ?? ageYears < 18;
  const flags: Flag[] = [DISCLAIMER];

  const pregnant = has(conditions, 'pregnancy');
  const breastfeeding = has(conditions, 'breastfeeding');

  // ---- Whether weight loss is allowed at all ----
  let weightLossBlocked = false;
  if (pregnant || breastfeeding) {
    weightLossBlocked = true;
    flags.push({
      code: 'NO_DEFICIT_PREGNANCY',
      severity: 'critical',
      message: pregnant
        ? 'Weight-loss diets are not appropriate during pregnancy — calories increased instead.'
        : 'Weight-loss diets are not appropriate while breastfeeding — calories increased instead.',
    });
  }
  if (isMinor) {
    weightLossBlocked = true;
    flags.push({
      code: 'MINOR_GROWTH',
      severity: 'warning',
      message:
        'For under-18s the plan targets healthy growth, not weight loss. Involve a guardian and clinician.',
    });
  }

  // ---- Calorie target ----
  let dailyKcal: number;
  let plannedWeeklyLossKg = 0;

  if (pregnant || breastfeeding) {
    dailyKcal = tdee + (pregnant ? PREGNANCY_KCAL : 0) + (breastfeeding ? BREASTFEEDING_KCAL : 0);
  } else if (goal === 'gain') {
    // Lean surplus, capped ~0.35% body weight per week.
    const weeklyGain = round(0.0035 * currentWeightKg, 3);
    dailyKcal = round(tdee + (weeklyGain * KCAL_PER_KG_FAT) / 7, 0);
    plannedWeeklyLossKg = -weeklyGain;
  } else if (goal === 'lose' && !weightLossBlocked) {
    const cap = MAX_WEEKLY_LOSS_FRACTION * currentWeightKg;
    let desired = input.desiredWeeklyLossKg ?? cap;
    if (desired > cap) {
      flags.push({
        code: 'WEEKLY_LOSS_CAPPED',
        severity: 'warning',
        message: `Weekly loss capped at ${round(cap, 2)} kg (~0.75% body weight) for safety; timeline extended.`,
      });
      desired = cap;
    }
    let deficit = (desired * KCAL_PER_KG_FAT) / 7;
    const maxDeficit = MAX_DEFICIT_PCT * tdee;
    if (deficit > maxDeficit) {
      deficit = maxDeficit;
      flags.push({
        code: 'DEFICIT_CAPPED',
        severity: 'info',
        message: 'Deficit capped at 25% of maintenance calories; timeline extended.',
      });
    }
    dailyKcal = round(tdee - deficit, 0);
  } else {
    // maintain, or a lose-goal that is blocked -> maintenance
    dailyKcal = tdee;
  }

  // ---- Calorie floor ----
  const floor = CALORIE_FLOOR[sex];
  if (dailyKcal < floor) {
    if (clinicianOverride) {
      flags.push({
        code: 'BELOW_FLOOR_CLINICIAN',
        severity: 'critical',
        message: `Calorie target is below the usual ${floor} kcal floor; permitted only under clinician supervision.`,
      });
    } else {
      flags.push({
        code: 'CALORIE_FLOOR_APPLIED',
        severity: 'warning',
        message: `Calorie target raised to the ${floor} kcal floor; weight-loss timeline extended to stay safe.`,
      });
      dailyKcal = floor;
    }
  }

  // Actual planned weekly change from the final calorie delta.
  const dailyDelta = tdee - dailyKcal; // + = deficit (loss)
  const safeWeeklyDeltaKg = round(-(dailyDelta * 7) / KCAL_PER_KG_FAT, 2);
  void plannedWeeklyLossKg;

  // ---- Macro overrides by condition ----
  let proteinPerKg = 1.8;
  let fatPerKgMin = 0.6;
  let sodiumMaxMg: number | undefined;

  if (has(conditions, 'pcos')) {
    proteinPerKg = 2.0;
    flags.push({
      code: 'PCOS_LOWGI',
      severity: 'info',
      message: 'PCOS/PCOD: favouring lower-GI carbs and higher protein.',
    });
  }

  if (has(conditions, 'kidney_disease')) {
    proteinPerKg = 0.7; // overrides any high-protein preference
    flags.push({
      code: 'CKD_PROTEIN_LOWERED',
      severity: 'critical',
      message:
        'Kidney disease: protein lowered to ~0.7 g/kg and potassium/phosphorus watched. Requires medical supervision.',
    });
  }

  if (has(conditions, 'diabetes')) {
    flags.push({
      code: 'DIABETES_LOWGI',
      severity: 'warning',
      message:
        'Diabetes: low-GI carbs distributed across meals, higher fibre. Coordinate carb timing with medication.',
    });
  }

  if (has(conditions, 'hypertension')) {
    sodiumMaxMg = 2000;
    flags.push({
      code: 'HTN_SODIUM',
      severity: 'warning',
      message: 'Hypertension: DASH-style plan, sodium limited to ~1500–2000 mg/day.',
    });
  }

  if (has(conditions, 'heart_disease')) {
    sodiumMaxMg = Math.min(sodiumMaxMg ?? Infinity, 1500);
    flags.push({
      code: 'HEART_SATFAT_SODIUM',
      severity: 'warning',
      message: 'Heart disease: saturated/trans fat minimised and sodium limited to ~1500 mg/day.',
    });
  }

  if (has(conditions, 'fatty_liver')) {
    flags.push({
      code: 'FATTY_LIVER_SUGAR',
      severity: 'warning',
      message: 'Fatty liver: added sugar, refined carbs and alcohol cut.',
    });
  }

  if (has(conditions, 'thyroid')) {
    flags.push({
      code: 'THYROID_PACE',
      severity: 'info',
      message: 'Thyroid: realistic pace; take medication per your doctor and separate from certain foods.',
    });
  }

  if (has(conditions, 'gout')) {
    flags.push({
      code: 'GOUT_PURINES',
      severity: 'info',
      message: 'Gout: high-purine foods (organ meats, certain seafood) limited.',
    });
  }

  if (reducedMobility) {
    flags.push({
      code: 'REDUCED_MOBILITY',
      severity: 'info',
      message:
        'Reduced mobility: deficit comes mainly from diet; movement kept low-impact (chair/aquatic/physio).',
    });
  }

  // ---- Medical-supervision banner ----
  const supervisionReasons: string[] = [];
  if (bmi >= 35) supervisionReasons.push('BMI ≥ 35');
  if (bmi < 17) supervisionReasons.push('BMI < 17');
  if (has(conditions, 'diabetes')) supervisionReasons.push('diabetes');
  if (has(conditions, 'kidney_disease')) supervisionReasons.push('kidney disease');
  if (has(conditions, 'heart_disease')) supervisionReasons.push('heart disease');
  if (pregnant) supervisionReasons.push('pregnancy');
  if (breastfeeding) supervisionReasons.push('breastfeeding');
  if (has(conditions, 'cancer')) supervisionReasons.push('cancer');
  if (has(conditions, 'post_surgery')) supervisionReasons.push('post-surgery recovery');

  const requiresSupervision = supervisionReasons.length > 0;
  if (requiresSupervision) {
    flags.push({
      code: 'REQUIRES_SUPERVISION',
      severity: 'critical',
      message: `Please work with a doctor for this plan (${supervisionReasons.join(', ')}).`,
    });
  }

  return {
    dailyKcal,
    proteinPerKg,
    fatPerKgMin,
    sodiumMaxMg,
    safeWeeklyDeltaKg,
    requiresSupervision,
    weightLossBlocked,
    flags,
  };
}
