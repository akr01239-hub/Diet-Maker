import { describe, it, expect } from 'vitest';
import { computeCycle, cycleGuidance } from '../src/modules/cycle/cycle';

const start = new Date('2026-06-01T00:00:00Z');
const plus = (days: number) => new Date(start.getTime() + days * 86_400_000);

describe('computeCycle', () => {
  it('is menstrual on the first days', () => {
    const s = computeCycle(start, plus(1), 28, 5);
    expect(s.cycleDay).toBe(2);
    expect(s.phase).toBe('menstrual');
  });

  it('is follicular after the period', () => {
    expect(computeCycle(start, plus(8), 28, 5).phase).toBe('follicular');
  });

  it('is ovulation around cycleLen − 14', () => {
    expect(computeCycle(start, plus(13), 28, 5).phase).toBe('ovulation');
  });

  it('is luteal in the back half', () => {
    expect(computeCycle(start, plus(20), 28, 5).phase).toBe('luteal');
  });

  it('prompts to confirm a new period from ~day 25', () => {
    expect(computeCycle(start, plus(23), 28, 5).shouldPromptPeriod).toBe(false);
    expect(computeCycle(start, plus(25), 28, 5).shouldPromptPeriod).toBe(true);
  });

  it('every phase has diet, exercise and yoga guidance', () => {
    for (const p of ['menstrual', 'follicular', 'ovulation', 'luteal'] as const) {
      const g = cycleGuidance(p);
      expect(g.dietTips.length).toBeGreaterThan(0);
      expect(g.exerciseTips.length).toBeGreaterThan(0);
      expect(g.yogaTips.length).toBeGreaterThan(0);
    }
  });

  it('gives craving-buster tips in the period and pre-period phases', () => {
    expect(cycleGuidance('menstrual').cravingTips.length).toBeGreaterThan(0);
    expect(cycleGuidance('luteal').cravingTips.length).toBeGreaterThan(0);
    expect(cycleGuidance('follicular').cravingTips.length).toBe(0);
  });
});
