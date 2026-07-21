import { describe, it, expect } from 'vitest';
import {
  computeStreak,
  sumTotals,
  weightTrend,
  buildDashboard,
  dayKey,
} from '../src/modules/logging/dashboard';

describe('sumTotals', () => {
  it('adds and rounds nutrition entries', () => {
    const t = sumTotals([
      { kcal: 200, proteinG: 10, carbG: 20, fatG: 5, fiberG: 3, sodiumMg: 100 },
      { kcal: 150.4, proteinG: 5.55, carbG: 10, fatG: 2, fiberG: 1, sodiumMg: 50 },
    ]);
    expect(t.kcal).toBe(350);
    expect(t.proteinG).toBe(15.6);
    expect(t.sodiumMg).toBe(150);
  });

  it('handles no entries', () => {
    expect(sumTotals([]).kcal).toBe(0);
  });
});

describe('computeStreak', () => {
  const today = '2026-07-21';
  it('counts consecutive days ending today', () => {
    expect(computeStreak(['2026-07-21', '2026-07-20', '2026-07-19'], today)).toBe(3);
  });

  it('breaks on a gap', () => {
    expect(computeStreak(['2026-07-21', '2026-07-19'], today)).toBe(1);
  });

  it('keeps the streak alive if today is not logged yet but yesterday is', () => {
    expect(computeStreak(['2026-07-20', '2026-07-19'], today)).toBe(2);
  });

  it('is 0 when the last log is older than yesterday', () => {
    expect(computeStreak(['2026-07-18'], today)).toBe(0);
  });

  it('is 0 with no logs', () => {
    expect(computeStreak([], today)).toBe(0);
  });
});

describe('weightTrend', () => {
  it('computes first, latest and delta from unordered points', () => {
    const t = weightTrend([
      { date: '2026-07-15T00:00:00Z', weightKg: 80 },
      { date: '2026-07-01T00:00:00Z', weightKg: 83 },
      { date: '2026-07-08T00:00:00Z', weightKg: 81.5 },
    ]);
    expect(t.firstKg).toBe(83);
    expect(t.latestKg).toBe(80);
    expect(t.deltaKg).toBe(-3);
    expect(t.points).toHaveLength(3);
  });

  it('handles no check-ins', () => {
    expect(weightTrend([]).deltaKg).toBeNull();
  });
});

describe('buildDashboard', () => {
  it('computes progress vs targets', () => {
    const d = buildDashboard({
      targets: { dailyKcal: 2000, proteinG: 120, waterMl: 2500 },
      todayTotals: { kcal: 1200, proteinG: 60, carbG: 150, fatG: 40, fiberG: 20, sodiumMg: 1500 },
      waterTodayMl: 1250,
      logDayKeys: ['2026-07-21', '2026-07-20'],
      todayKey: '2026-07-21',
      weightPoints: [{ date: '2026-07-01T00:00:00Z', weightKg: 82 }],
      bmi: 26.1,
    });
    expect(d.calories.remaining).toBe(800);
    expect(d.calories.percent).toBe(60);
    expect(d.protein.percent).toBe(50);
    expect(d.water.percent).toBe(50);
    expect(d.streakDays).toBe(2);
    expect(d.bmi).toBe(26.1);
  });

  it('degrades gracefully with no targets yet', () => {
    const d = buildDashboard({
      targets: null,
      todayTotals: { kcal: 500, proteinG: 20, carbG: 60, fatG: 15, fiberG: 8, sodiumMg: 400 },
      waterTodayMl: 0,
      logDayKeys: [],
      todayKey: '2026-07-21',
      weightPoints: [],
    });
    expect(d.calories.target).toBeNull();
    expect(d.calories.remaining).toBeNull();
    expect(d.streakDays).toBe(0);
  });
});

describe('dayKey', () => {
  it('returns a UTC YYYY-MM-DD', () => {
    expect(dayKey(new Date('2026-07-21T15:30:00Z'))).toBe('2026-07-21');
  });
});
