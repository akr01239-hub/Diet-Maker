import { describe, it, expect } from 'vitest';
import {
  micronutrientTargets,
  assessMicronutrients,
  type Micronutrients,
} from '../src/calc/micronutrients';

const full: Micronutrients = {
  ironMg: 18,
  calciumMg: 1000,
  vitaminB12Mcg: 2.4,
  vitaminDMcg: 15,
  folateMcg: 400,
  potassiumMg: 3400,
  magnesiumMg: 400,
};

describe('micronutrientTargets', () => {
  it('gives pre-menopausal women more iron', () => {
    expect(micronutrientTargets('female', 30).ironMg).toBe(18);
    expect(micronutrientTargets('male', 30).ironMg).toBe(8);
    expect(micronutrientTargets('female', 55).ironMg).toBe(8); // post-menopausal
  });

  it('raises calcium after 50 and vitamin D after 70', () => {
    expect(micronutrientTargets('male', 60).calciumMg).toBe(1200);
    expect(micronutrientTargets('male', 40).calciumMg).toBe(1000);
    expect(micronutrientTargets('female', 72).vitaminDMcg).toBe(20);
  });
});

describe('assessMicronutrients', () => {
  it('meeting every RDA yields ~100% coverage and no deficiencies', () => {
    const r = assessMicronutrients(full, 'female', 30);
    expect(r.coveragePct).toBe(100);
    expect(r.deficiencies).toHaveLength(0);
    expect(r.nutrients.every((n) => !n.low)).toBe(true);
  });

  it('no intake logged surfaces every nutrient as low', () => {
    const r = assessMicronutrients({}, 'male', 30);
    expect(r.coveragePct).toBe(0);
    expect(r.deficiencies.length).toBe(r.nutrients.length);
  });

  it('flags a specific gap and orders deficiencies worst-first', () => {
    const r = assessMicronutrients({ ...full, ironMg: 4, calciumMg: 300 }, 'female', 30);
    expect(r.deficiencies).toContain('ironMg'); // 4/18 = 22%
    expect(r.deficiencies).toContain('calciumMg'); // 300/1000 = 30%
    // iron (22%) is worse than calcium (30%) → comes first
    expect(r.deficiencies.indexOf('ironMg')).toBeLessThan(r.deficiencies.indexOf('calciumMg'));
  });

  it('caps per-nutrient contribution so mega-dosing one does not mask gaps', () => {
    const r = assessMicronutrients({ ...full, potassiumMg: 34000, ironMg: 0 }, 'male', 30);
    expect(r.nutrients.find((n) => n.key === 'ironMg')!.low).toBe(true);
    expect(r.coveragePct).toBeLessThan(100); // iron gap still drags it down
  });
});
