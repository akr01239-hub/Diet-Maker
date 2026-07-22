import { describe, it, expect } from 'vitest';
import { applyGuardrails } from '../src/guardrails';
import type { GuardrailInput } from '../src/guardrails';

const base: GuardrailInput = {
  sex: 'female',
  ageYears: 30,
  currentWeightKg: 60,
  targetWeightKg: 55,
  heightCm: 165,
  tdee: 2000,
  bmi: 22,
  goal: 'lose',
  conditions: [],
};

const codes = (r: ReturnType<typeof applyGuardrails>) => r.flags.map((f) => f.code);

describe('guardrails safety gates', () => {
  it('refuses weight loss when already underweight (BMI < 18.5)', () => {
    const r = applyGuardrails({ ...base, bmi: 17.5, currentWeightKg: 47, targetWeightKg: 44 });
    expect(r.weightLossBlocked).toBe(true);
    expect(r.dailyKcal).toBe(2000); // maintenance, not a deficit
    expect(codes(r)).toContain('UNDERWEIGHT_NO_LOSS');
  });

  it('refuses a loss goal toward an underweight target BMI', () => {
    // 165cm, 48kg target => BMI ~17.6
    const r = applyGuardrails({ ...base, targetWeightKg: 48 });
    expect(r.weightLossBlocked).toBe(true);
    expect(codes(r)).toContain('UNDERWEIGHT_NO_LOSS');
  });

  it('blocks a calorie deficit for a cancer diagnosis', () => {
    const r = applyGuardrails({ ...base, conditions: ['cancer'] });
    expect(r.weightLossBlocked).toBe(true);
    expect(r.dailyKcal).toBeGreaterThanOrEqual(2000);
    expect(codes(r)).toContain('CANCER_NO_DEFICIT');
  });

  it('warns about dialysis protein + fluid for kidney disease', () => {
    const r = applyGuardrails({ ...base, goal: 'maintain', conditions: ['kidney_disease'] });
    expect(codes(r)).toContain('CKD_PROTEIN_LOWERED');
    expect(codes(r)).toContain('FLUID_RESTRICTION');
    expect(r.flags.find((f) => f.code === 'CKD_PROTEIN_LOWERED')!.message.toLowerCase()).toContain('dialysis');
  });

  it('allows a normal loss goal in the healthy range', () => {
    const r = applyGuardrails(base);
    expect(r.weightLossBlocked).toBe(false);
    expect(r.dailyKcal).toBeLessThan(2000);
  });
});
