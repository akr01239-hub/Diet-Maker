import { requireCompleteProfile } from '../profile/profile.service';
import { ageFromDob } from '../nutrition/calc.service';
import { bmi as bmiCalc } from '../../calc/anthropometry';
import { assessRisks, type RiskAssessment } from './risk';

/**
 * Runs the deterministic health-risk engine over the user's stored profile numbers
 * (waist, BP, fasting glucose, resting HR) plus their computed BMI. Sleep/hydration are
 * optional and can be passed in from the client (Health Connect / today's water %).
 */
export async function getRiskAssessment(
  userId: string,
  extra?: { sleepHours?: number; hydrationPct?: number },
): Promise<RiskAssessment> {
  const { profile, sensitive } = await requireCompleteProfile(userId);
  const bmi = bmiCalc(sensitive.currentWeightKg, profile.heightCm);
  return assessRisks({
    sex: sensitive.sex,
    ageYears: ageFromDob(sensitive.dob),
    bmi,
    heightCm: profile.heightCm,
    waistCm: sensitive.waistCm,
    bloodPressure: sensitive.bloodPressure,
    bloodSugar: sensitive.bloodSugar,
    restingHr: sensitive.restingHr,
    conditions: sensitive.conditions,
    sleepHours: extra?.sleepHours,
    hydrationPct: extra?.hydrationPct,
  });
}
