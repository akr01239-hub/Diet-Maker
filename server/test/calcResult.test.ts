import { describe, it, expect } from 'vitest';
import { computeCalcResult } from '../src/modules/nutrition/calcResult';

describe('computeCalcResult — end-to-end deterministic pipeline', () => {
  it('produces a coherent plan for a healthy adult male losing weight', () => {
    const r = computeCalcResult({
      heightCm: 180,
      currentWeightKg: 90,
      targetWeightKg: 78,
      ageYears: 32,
      sex: 'male',
      activityLevel: 'moderate',
      goal: 'lose',
      waistCm: 96,
    });

    expect(r.bmi).toBe(27.8);
    expect(r.bmr).toBe(1870); // 900 + 1125 - 160 + 5
    expect(r.tdee).toBe(Math.round(1870 * 1.55));
    expect(r.dailyKcal).toBeLessThan(r.tdee); // a deficit
    expect(r.dailyKcal).toBeGreaterThanOrEqual(1500); // male floor respected
    expect(r.proteinG).toBeGreaterThan(0);
    // macros roughly reconstruct the calorie target (fiber/rounding slack).
    const kcalFromMacros = r.proteinG * 4 + r.carbG * 4 + r.fatG * 9;
    expect(Math.abs(kcalFromMacros - r.dailyKcal)).toBeLessThanOrEqual(20);
    expect(r.requiresSupervision).toBe(false);
  });

  it('CKD patient gets lowered protein and a supervision flag end-to-end', () => {
    const r = computeCalcResult({
      heightCm: 170,
      currentWeightKg: 80,
      targetWeightKg: 72,
      ageYears: 55,
      sex: 'male',
      activityLevel: 'light',
      goal: 'lose',
      conditions: ['kidney_disease'],
    });
    // CKD caps protein at 0.7 g/kg, scaled to the obesity-adjusted body weight (not the
    // lower target): idealMax(22.9 BMI @170cm)=66.2, ABW=66.2+0.25*(80-66.2)=69.7 -> ~49 g.
    const idealMax = Math.round(22.9 * 1.7 * 1.7 * 10) / 10;
    const abw = Math.round((idealMax + 0.25 * (80 - idealMax)) * 10) / 10;
    expect(r.proteinG).toBeCloseTo(Math.round(0.7 * abw), 0);
    expect(r.requiresSupervision).toBe(true);
    expect(r.flags.some((f) => f.code === 'CKD_PROTEIN_LOWERED')).toBe(true);
  });

  it('pregnant user never gets a deficit', () => {
    const r = computeCalcResult({
      heightCm: 165,
      currentWeightKg: 68,
      targetWeightKg: 60,
      ageYears: 29,
      sex: 'female',
      activityLevel: 'light',
      goal: 'lose',
      conditions: ['pregnancy'],
    });
    expect(r.weightLossBlocked).toBe(true);
    expect(r.dailyKcal).toBeGreaterThan(r.tdee);
  });
});
