import { describe, it, expect } from 'vitest';
import { buildGroceryList } from '../src/modules/reports/grocery';
import { buildWeeklyReport, reportToCsv } from '../src/modules/reports/report';
import { computeBadges, badgeSummary } from '../src/modules/reports/gamification';
import { SEED_FOODS } from '../src/data/foods.seed';
import { generateWeekPlan } from '../src/modules/food/planGenerator';
import type { PlanTargets } from '../src/modules/food/food.types';

const targets: PlanTargets = { dailyKcal: 2000, proteinG: 130, fatG: 60, carbG: 220, fiberG: 28 };

describe('grocery list', () => {
  const plan = generateWeekPlan(SEED_FOODS, targets, { dietType: 'veg', allergies: [], conditions: [] });

  it('aggregates grams per food across the week', () => {
    const g = buildGroceryList(plan.days, SEED_FOODS);
    expect(g.items.length).toBeGreaterThan(0);
    expect(g.items.every((i) => i.totalGrams > 0)).toBe(true);
    // sorted by name
    const names = g.items.map((i) => i.name);
    expect(names).toEqual([...names].sort((a, b) => a.localeCompare(b)));
  });

  it('trims tier-3 items to fit a tight budget', () => {
    const full = buildGroceryList(plan.days, SEED_FOODS);
    const tight = buildGroceryList(plan.days, SEED_FOODS, {
      monthlyBudget: full.estimatedCostTierTotal * 0.5,
    });
    expect(tight.estimatedCostTierTotal).toBeLessThanOrEqual(full.estimatedCostTierTotal);
    expect(tight.items.length).toBeLessThanOrEqual(full.items.length);
  });
});

describe('weekly report', () => {
  const report = buildWeeklyReport({
    name: 'Test User',
    generatedAt: '2026-07-21T00:00:00Z',
    targets: { dailyKcal: 2000, proteinG: 130, waterMl: 2500 },
    bmi: 27.8,
    latestWeightKg: 80,
    weightDeltaKg: -2,
    days: [
      { date: '2026-07-19', kcal: 1900, proteinG: 120 },
      { date: '2026-07-20', kcal: 2100, proteinG: 130 },
    ],
  });

  it('computes averages and adherence', () => {
    expect(report.avgKcal).toBe(2000);
    expect(report.adherencePct).toBe(100);
    expect(report.disclaimer).toContain('not medical advice');
  });

  it('renders CSV with a header and rows', () => {
    const csv = reportToCsv(report);
    expect(csv).toContain('NutriAI Weekly Report');
    expect(csv).toContain('2026-07-19,1900,120');
    expect(csv.split('\n').length).toBeGreaterThan(5);
  });

  it('handles a user with no logged days', () => {
    const empty = buildWeeklyReport({
      name: 'X', generatedAt: 'now', targets: null, bmi: null,
      latestWeightKg: null, weightDeltaKg: null, days: [],
    });
    expect(empty.avgKcal).toBeNull();
    expect(empty.adherencePct).toBeNull();
  });
});

describe('gamification', () => {
  it('awards badges by threshold', () => {
    const badges = computeBadges({
      totalFoodLogs: 12,
      streakDays: 7,
      metWaterTargetToday: true,
      metProteinTargetToday: false,
      weightLostKg: 1.5,
      checkinCount: 2,
    });
    const earned = new Set(badges.filter((b) => b.earned).map((b) => b.code));
    expect(earned.has('first_log')).toBe(true);
    expect(earned.has('streak_7')).toBe(true);
    expect(earned.has('streak_30')).toBe(false);
    expect(earned.has('hydrated')).toBe(true);
    expect(earned.has('protein_pro')).toBe(false);
    expect(earned.has('down_1kg')).toBe(true);
    expect(earned.has('down_5kg')).toBe(false);
  });

  it('summarises earned vs total', () => {
    const s = badgeSummary(
      computeBadges({
        totalFoodLogs: 0, streakDays: 0, metWaterTargetToday: false,
        metProteinTargetToday: false, weightLostKg: 0, checkinCount: 0,
      }),
    );
    expect(s.earnedCount).toBe(0);
    expect(s.total).toBeGreaterThan(0);
  });
});
