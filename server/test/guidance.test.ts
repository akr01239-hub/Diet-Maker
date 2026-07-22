import { describe, it, expect } from 'vitest';
import { buildGuidance } from '../src/modules/guidance/guidance';

const base = {
  sex: 'male',
  ageYears: 30,
  goal: 'maintain' as const,
  activityLevel: 'moderate',
  conditions: [] as string[],
};

describe('buildGuidance', () => {
  it('is never empty (fallbacks)', () => {
    const g = buildGuidance(base);
    expect(g.dietTips.length).toBeGreaterThan(0);
    expect(g.exerciseTips.length).toBeGreaterThan(0);
  });

  it('adds fatty-liver diet + exercise guidance', () => {
    const g = buildGuidance({ ...base, conditions: ['fatty_liver'] });
    expect(g.summary.toLowerCase()).toContain('fatty liver');
    expect(g.dietTips.join(' ').toLowerCase()).toContain('sugar');
    expect(g.exerciseTips.join(' ').toLowerCase()).toContain('cardio');
  });

  it('adds PCOS insulin-focused guidance', () => {
    const g = buildGuidance({ ...base, sex: 'female', conditions: ['pcos'] });
    expect(g.dietTips.join(' ').toLowerCase()).toContain('low-gi');
    expect(g.exerciseTips.join(' ').toLowerCase()).toContain('strength');
  });

  it('gives desk-job lifestyle tips for a sedentary user', () => {
    const g = buildGuidance({ ...base, activityLevel: 'sedentary' });
    expect(g.exerciseTips.join(' ').toLowerCase()).toContain('step');
    expect(g.dietTips.join(' ').toLowerCase()).toContain('portion');
  });

  it('gives sex-specific nutrition tips', () => {
    const women = buildGuidance({ ...base, sex: 'female' });
    expect(women.dietTips.join(' ').toLowerCase()).toContain('iron');
    const men = buildGuidance({ ...base, sex: 'male' });
    expect(men.dietTips.join(' ')).not.toEqual(women.dietTips.join(' '));
  });

  it('caps tips so the card stays scannable', () => {
    const g = buildGuidance({
      ...base,
      sex: 'female',
      activityLevel: 'sedentary',
      goal: 'lose',
      conditions: ['fatty_liver', 'pcos', 'diabetes', 'hypertension'],
    });
    expect(g.dietTips.length).toBeLessThanOrEqual(6);
    expect(g.exerciseTips.length).toBeLessThanOrEqual(5);
  });
});
