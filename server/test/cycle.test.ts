import { describe, it, expect } from 'vitest';
import { computeCycle, cycleGuidance, analyzeCycleHealth } from '../src/modules/cycle/cycle';

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

  it('gives PMS management only in the pre-period (luteal) phase', () => {
    expect(cycleGuidance('luteal').pmsTips.length).toBeGreaterThan(0);
    expect(cycleGuidance('luteal').pmsTips.join(' ').toLowerCase()).toContain('pms');
    expect(cycleGuidance('menstrual').pmsTips.length).toBe(0);
    expect(cycleGuidance('ovulation').pmsTips.length).toBe(0);
  });
});

describe('analyzeCycleHealth', () => {
  const ok = { cycleLengths: [28, 29, 28], periodLengths: [5, 5], daysSinceLastStart: 10, avgCycleLength: 28 };

  it('needs data before analysing', () => {
    const h = analyzeCycleHealth({ cycleLengths: [], periodLengths: [], daysSinceLastStart: 5, avgCycleLength: 28 });
    expect(h.status).toBe('insufficient_data');
    expect(h.advice.diet.length).toBeGreaterThan(0);
  });

  it('flags a healthy cycle as ok', () => {
    const h = analyzeCycleHealth(ok);
    expect(h.status).toBe('ok');
    expect(h.seeDoctor).toBe(false);
  });

  it('flags a very short (2-day) period as a concern', () => {
    const h = analyzeCycleHealth({ ...ok, periodLengths: [2] });
    expect(h.status).toBe('concern');
    expect(h.seeDoctor).toBe(true);
    expect(h.findings.some((f) => /short period/i.test(f.issue))).toBe(true);
  });

  it('flags irregular cycles', () => {
    const h = analyzeCycleHealth({ ...ok, cycleLengths: [24, 40, 30], avgCycleLength: 31 });
    expect(h.findings.some((f) => /irregular/i.test(f.issue))).toBe(true);
  });

  it('always returns diet/sleep/lifestyle/exercise advice with a disclaimer', () => {
    const h = analyzeCycleHealth({ ...ok, periodLengths: [2], smoking: 'regular' });
    expect(h.advice.diet.length).toBeGreaterThan(0);
    expect(h.advice.sleep.length).toBeGreaterThan(0);
    expect(h.advice.lifestyle.join(' ').toLowerCase()).toContain('smoking');
    expect(h.disclaimer.toLowerCase()).toContain('gynaecologist');
  });
});
