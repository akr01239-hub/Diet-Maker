import { describe, it, expect } from 'vitest';
import {
  bmi,
  bmiCategory,
  bmiResult,
  whtr,
  idealWeightRange,
  bodyFatDeurenberg,
  anthropometrySummary,
  bmrMifflinStJeor,
  tdee,
  energy,
  computeMacros,
  ACTIVITY_FACTORS,
  type Anthropometrics,
} from '../src/calc';

describe('BMI', () => {
  it('computes kg/m² correctly', () => {
    // 70kg, 175cm -> 22.86
    expect(bmi(70, 175)).toBe(22.9);
    // 100kg, 180cm -> 30.86
    expect(bmi(100, 180)).toBe(30.9);
  });

  it('categorises per WHO Asian-Indian bands by default', () => {
    expect(bmiCategory(17)).toBe('underweight');
    expect(bmiCategory(22)).toBe('normal');
    expect(bmiCategory(24)).toBe('overweight'); // 23–24.9 = overweight for Asians
    expect(bmiCategory(27)).toBe('obese'); // ≥25 = obese for Asians
    // boundaries
    expect(bmiCategory(18.5)).toBe('normal');
    expect(bmiCategory(23)).toBe('overweight');
    expect(bmiCategory(25)).toBe('obese');
  });

  it('categorises per international bands when asian=false', () => {
    expect(bmiCategory(24, false)).toBe('normal');
    expect(bmiCategory(27, false)).toBe('overweight');
    expect(bmiCategory(32, false)).toBe('obese');
    expect(bmiCategory(25, false)).toBe('overweight');
    expect(bmiCategory(30, false)).toBe('obese');
  });

  it('returns a combined result', () => {
    expect(bmiResult(70, 175)).toEqual({ bmi: 22.9, category: 'normal' });
  });

  it('rejects invalid input', () => {
    expect(() => bmi(70, 0)).toThrow(RangeError);
    expect(() => bmi(0, 175)).toThrow(RangeError);
  });
});

describe('WHtR', () => {
  it('is waist/height', () => {
    expect(whtr(80, 175)).toBe(0.46);
  });
  it('is undefined without waist', () => {
    expect(whtr(undefined, 175)).toBeUndefined();
  });
  it('rejects invalid height', () => {
    expect(() => whtr(80, 0)).toThrow(RangeError);
  });
});

describe('ideal weight range', () => {
  it('derives from healthy BMI band (Asian upper bound by default)', () => {
    // 175cm: 18.5..22.9 * 1.75^2 -> 56.7 .. 70.1
    expect(idealWeightRange(175)).toEqual({ minKg: 56.7, maxKg: 70.1 });
    // international band 18.5..24.9 -> 56.7 .. 76.3
    expect(idealWeightRange(175, false)).toEqual({ minKg: 56.7, maxKg: 76.3 });
  });
  it('rejects invalid height', () => {
    expect(() => idealWeightRange(0)).toThrow(RangeError);
  });
});

describe('body fat (Deurenberg)', () => {
  it('estimates for male vs female', () => {
    // BMI 22.9, age 30, male: 1.2*22.9 + 0.23*30 - 10.8 - 5.4 = 18.18 -> 18.2
    expect(bodyFatDeurenberg({ bmi: 22.9, ageYears: 30, sex: 'male' })).toBe(18.2);
    // female (sexFactor 0) is 10.8 higher -> 28.98 -> 29.0
    expect(bodyFatDeurenberg({ bmi: 22.9, ageYears: 30, sex: 'female' })).toBe(29.0);
  });
  it('clamps to a sane range', () => {
    expect(bodyFatDeurenberg({ bmi: 10, ageYears: 18, sex: 'male' })).toBeGreaterThanOrEqual(3);
    expect(bodyFatDeurenberg({ bmi: 60, ageYears: 80, sex: 'female' })).toBeLessThanOrEqual(65);
  });
});

describe('BMR (Mifflin-St Jeor)', () => {
  const male: Anthropometrics = { weightKg: 80, heightCm: 180, ageYears: 30, sex: 'male' };
  const female: Anthropometrics = { weightKg: 65, heightCm: 165, ageYears: 30, sex: 'female' };

  it('male formula: 10W+6.25H-5A+5', () => {
    // 800 + 1125 - 150 + 5 = 1780
    expect(bmrMifflinStJeor(male)).toBe(1780);
  });

  it('female formula: 10W+6.25H-5A-161', () => {
    // 650 + 1031.25 - 150 - 161 = 1370.25 -> 1370
    expect(bmrMifflinStJeor(female)).toBe(1370);
  });

  it('rejects non-positive inputs', () => {
    expect(() => bmrMifflinStJeor({ ...male, weightKg: 0 })).toThrow(RangeError);
    expect(() => bmrMifflinStJeor({ ...male, ageYears: 0 })).toThrow(RangeError);
  });
});

describe('TDEE', () => {
  it('multiplies BMR by activity factor', () => {
    expect(tdee(1780, 'sedentary')).toBe(Math.round(1780 * 1.2));
    expect(tdee(1780, 'moderate')).toBe(Math.round(1780 * 1.55));
    expect(tdee(1780, 'veryactive')).toBe(Math.round(1780 * 1.9));
  });

  it('exposes the documented factors', () => {
    expect(ACTIVITY_FACTORS).toEqual({
      sedentary: 1.2,
      light: 1.375,
      moderate: 1.55,
      active: 1.725,
      veryactive: 1.9,
    });
  });

  it('energy() combines bmr + tdee', () => {
    const r = energy({ weightKg: 80, heightCm: 180, ageYears: 30, sex: 'male' }, 'moderate');
    expect(r.bmr).toBe(1780);
    expect(r.tdee).toBe(Math.round(1780 * 1.55));
  });
});

describe('macros', () => {
  it('splits kcal into protein/fat/carb + fiber + water', () => {
    const m = computeMacros({
      dailyKcal: 2000,
      targetWeightKg: 70,
      currentWeightKg: 75,
      proteinPerKg: 1.8,
      fatPerKgMin: 0.6,
      waterPerKg: 33,
    });
    expect(m.proteinG).toBe(126); // 1.8*70
    // fat: max(0.6*70=42, 0.2*2000/9=44.4) -> 44
    expect(m.fatG).toBe(44);
    // carbs: (2000 - 126*4 - 44*9)/4 = (2000-504-396)/4 = 275
    expect(m.carbG).toBe(275);
    expect(m.fiberG).toBe(28); // 14 * 2
    expect(m.waterMl).toBe(2475); // 33*75
  });

  it('never returns negative carbs when protein+fat exceed kcal', () => {
    const m = computeMacros({
      dailyKcal: 800,
      targetWeightKg: 120,
      currentWeightKg: 120,
      proteinPerKg: 2.2,
    });
    expect(m.carbG).toBeGreaterThanOrEqual(0);
  });

  it('honours the 20%-of-kcal fat floor for high body weights on low fat/kg', () => {
    const m = computeMacros({
      dailyKcal: 2500,
      targetWeightKg: 50,
      currentWeightKg: 50,
      fatPerKgMin: 0.6,
    });
    // 0.6*50=30 vs 0.2*2500/9=55.6 -> 56
    expect(m.fatG).toBe(56);
  });

  it('rejects non-positive kcal', () => {
    expect(() =>
      computeMacros({ dailyKcal: 0, targetWeightKg: 70, currentWeightKg: 70 }),
    ).toThrow(RangeError);
  });
});

describe('anthropometrySummary', () => {
  it('bundles all metrics', () => {
    const s = anthropometrySummary({
      weightKg: 70,
      heightCm: 175,
      ageYears: 30,
      sex: 'male',
      waistCm: 85,
    });
    expect(s.bmi).toBe(22.9);
    expect(s.category).toBe('normal');
    expect(s.whtr).toBe(0.49);
    expect(s.idealWeight).toEqual({ minKg: 56.7, maxKg: 70.1 });
    expect(s.bodyFatEstimate).toBeGreaterThan(0);
  });
});
