import { describe, it, expect } from 'vitest';
import { computeAdherence, type AdherenceInput } from '../src/modules/discipline/adherence';

const keys = (r: ReturnType<typeof computeAdherence>) => r.components.map((c) => c.key);
const comp = (r: ReturnType<typeof computeAdherence>, key: string) =>
  r.components.find((c) => c.key === key);

/** A "perfect" day: everything hit, workout scheduled+done, sleep in range, checked in. */
const perfect: AdherenceInput = {
  kcalConsumed: 2000,
  kcalTarget: 2000,
  waterMl: 2500,
  waterTargetMl: 2500,
  proteinG: 130,
  proteinTargetG: 130,
  workoutScheduled: true,
  workoutDone: true,
  wellnessDone: true,
  sleepHours: 8,
  checkedInToday: true,
};

describe('computeAdherence', () => {
  it('a perfect day scores ~100', () => {
    const r = computeAdherence(perfect);
    expect(r.score).toBe(100);
    expect(r.topActions).toHaveLength(0);
    // every component earns full points
    for (const c of r.components) expect(c.points).toBeCloseTo(c.maxPoints, 5);
  });

  it('empty input scores low but never crashes and has no components', () => {
    const r = computeAdherence({});
    expect(r.score).toBe(0);
    expect(r.components).toHaveLength(0);
    expect(r.topActions).toHaveLength(0);
    expect(Array.isArray(r.topActions)).toBe(true);
  });

  it('never penalises missing data — the score is normalised over only what is known', () => {
    // Only food, and it is perfect → 100, not diluted by the missing components.
    const r = computeAdherence({ kcalConsumed: 2000, kcalTarget: 2000 });
    expect(keys(r)).toEqual(['food']);
    expect(r.score).toBe(100);
  });

  it('excludes workout from the denominator when it was NOT scheduled', () => {
    const notScheduled = computeAdherence({
      kcalConsumed: 2000,
      kcalTarget: 2000,
      workoutScheduled: false,
      workoutDone: false,
    });
    expect(keys(notScheduled)).not.toContain('workout');
    // food is perfect and workout is excluded, so the day is still 100
    expect(notScheduled.score).toBe(100);

    // when scheduled but not done, it IS counted (and drags the score down)
    const scheduledMissed = computeAdherence({
      kcalConsumed: 2000,
      kcalTarget: 2000,
      workoutScheduled: true,
      workoutDone: false,
    });
    expect(keys(scheduledMissed)).toContain('workout');
    expect(comp(scheduledMissed, 'workout')?.points).toBe(0);
    expect(scheduledMissed.score).toBeLessThan(100);
  });

  it('gives full sleep credit in range and partial credit out of range', () => {
    const inRange = computeAdherence({ sleepHours: 8 });
    expect(comp(inRange, 'sleep')?.points).toBeCloseTo(comp(inRange, 'sleep')!.maxPoints, 5);
    expect(inRange.score).toBe(100);

    const short = computeAdherence({ sleepHours: 5 });
    const s = comp(short, 'sleep')!;
    expect(s.points).toBeGreaterThan(0); // partial, not zeroed
    expect(s.points).toBeLessThan(s.maxPoints);

    // way out of range → still floored at 0, never negative
    const none = computeAdherence({ sleepHours: 2 });
    expect(comp(none, 'sleep')?.points).toBeGreaterThanOrEqual(0);
  });

  it('surfaces the biggest gap as an encouraging top action', () => {
    // Food perfect, but no water at all → water is the biggest miss.
    const r = computeAdherence({
      kcalConsumed: 2000,
      kcalTarget: 2000,
      waterMl: 0,
      waterTargetMl: 2500,
    });
    expect(r.topActions.length).toBeGreaterThanOrEqual(1);
    expect(r.topActions.length).toBeLessThanOrEqual(2);
    expect(r.topActions[0].toLowerCase()).toContain('water');
    expect(r.score).toBeLessThan(100);
  });

  it('handles partial data with a sensible blend and explainable components', () => {
    const r = computeAdherence({
      kcalConsumed: 1000,
      kcalTarget: 2000, // 50% of target
      proteinG: 65,
      proteinTargetG: 130, // 50%
    });
    expect(keys(r).sort()).toEqual(['food', 'protein']);
    expect(r.score).toBeGreaterThan(0);
    expect(r.score).toBeLessThan(100);
    for (const c of r.components) {
      expect(c.detail).toBeTruthy();
      expect(c.points).toBeGreaterThanOrEqual(0);
      expect(c.points).toBeLessThanOrEqual(c.maxPoints);
    }
  });

  it('counts boolean wellness/check-in even when false (present = has data)', () => {
    const r = computeAdherence({ wellnessDone: false, checkedInToday: false });
    expect(keys(r).sort()).toEqual(['checkin', 'wellness']);
    expect(r.score).toBe(0); // both present but missed
    expect(comp(r, 'wellness')?.points).toBe(0);
  });
});
