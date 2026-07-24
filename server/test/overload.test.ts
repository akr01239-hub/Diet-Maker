import { describe, it, expect } from 'vitest';
import { recommendNextSession } from '../src/modules/exercise/overload';
import type { LoggedSet } from '../src/modules/exercise/overload';

/** Build a logged set with sensible defaults. */
function set(over: Partial<LoggedSet> & { date: string }): LoggedSet {
  return {
    exerciseName: 'Barbell bench press',
    weightKg: 40,
    reps: 8,
    sets: 3,
    ...over,
  };
}

describe('recommendNextSession — progression', () => {
  it('adds one 2.5 kg step when a weighted lift hit all reps/sets', () => {
    const history: LoggedSet[] = [
      set({ date: '2026-01-01', weightKg: 40, reps: 8, sets: 3 }),
      set({ date: '2026-01-04', weightKg: 40, reps: 8, sets: 3 }),
    ];
    const [rec] = recommendNextSession(history);
    expect(rec.exerciseName).toBe('Barbell bench press');
    expect(rec.suggestedWeightKg).toBe(42.5);
    expect(rec.suggestedReps).toBe(8);
    expect(rec.suggestedSets).toBe(3);
    expect(rec.deload).toBe(false);
    expect(rec.rationale).toMatch(/2\.5 kg/);
  });

  it('never makes an absurd jump — exactly one increment per session', () => {
    const history: LoggedSet[] = [
      set({ date: '2026-01-01', weightKg: 100, reps: 5, sets: 5 }),
      set({ date: '2026-01-03', weightKg: 100, reps: 5, sets: 5 }),
    ];
    const [rec] = recommendNextSession(history);
    expect(rec.suggestedWeightKg).toBe(102.5); // +2.5, not +5 or a percentage
    expect(rec.deload).toBe(false);
  });
});

describe('recommendNextSession — hold on a miss', () => {
  it('holds the load when the last session regressed vs the prior', () => {
    const history: LoggedSet[] = [
      set({ date: '2026-01-01', weightKg: 40, reps: 8, sets: 3 }),
      set({ date: '2026-01-04', weightKg: 40, reps: 6, sets: 3 }), // fewer reps → miss
    ];
    const [rec] = recommendNextSession(history);
    expect(rec.suggestedWeightKg).toBe(40); // held, not increased
    expect(rec.suggestedReps).toBe(6);
    expect(rec.deload).toBe(false);
    expect(rec.rationale).toMatch(/fell short|Holding/i);
  });
});

describe('recommendNextSession — deload', () => {
  it('triggers a ~10% deload every 4 weeks (default)', () => {
    const history: LoggedSet[] = [
      set({ date: '2026-01-01', weightKg: 100, reps: 10, sets: 3 }),
      set({ date: '2026-01-29', weightKg: 100, reps: 10, sets: 3 }), // exactly 4 weeks later
    ];
    const [rec] = recommendNextSession(history);
    expect(rec.deload).toBe(true);
    expect(rec.suggestedWeightKg).toBe(90); // 100 * 0.9
    expect(rec.suggestedReps).toBe(9); // 10 * 0.9
    expect(rec.rationale).toMatch(/deload/i);
  });

  it('respects a custom deloadEveryWeeks cadence', () => {
    const history: LoggedSet[] = [
      set({ date: '2026-01-01', weightKg: 50, reps: 10, sets: 3 }),
      set({ date: '2026-01-15', weightKg: 50, reps: 10, sets: 3 }), // 2 weeks later
    ];
    const [rec] = recommendNextSession(history, { deloadEveryWeeks: 2 });
    expect(rec.deload).toBe(true);
    expect(rec.suggestedWeightKg).toBe(45);
  });
});

describe('recommendNextSession — edge cases', () => {
  it('returns an empty array for empty history', () => {
    expect(recommendNextSession([])).toEqual([]);
  });

  it('progresses conservatively from a single session', () => {
    const history: LoggedSet[] = [set({ date: '2026-01-01', weightKg: 60, reps: 5, sets: 5 })];
    const out = recommendNextSession(history);
    expect(out).toHaveLength(1);
    expect(out[0]!.suggestedWeightKg).toBe(62.5);
    expect(out[0]!.deload).toBe(false);
  });

  it('bodyweight (null weight) adds a rep instead of load', () => {
    const history: LoggedSet[] = [
      set({ exerciseName: 'Push-ups', date: '2026-01-01', weightKg: null, reps: 15, sets: 3 }),
      set({ exerciseName: 'Push-ups', date: '2026-01-03', weightKg: null, reps: 15, sets: 3 }),
    ];
    const [rec] = recommendNextSession(history);
    expect(rec.suggestedWeightKg).toBeNull();
    expect(rec.suggestedReps).toBe(16); // +1 rep
    expect(rec.suggestedSets).toBe(3);
    expect(rec.deload).toBe(false);
    expect(rec.rationale).toMatch(/rep/i);
  });

  it('is deterministic and groups multiple exercises, sorted by name', () => {
    const history: LoggedSet[] = [
      set({ exerciseName: 'Squat', date: '2026-01-01', weightKg: 80, reps: 5, sets: 5 }),
      set({ exerciseName: 'Deadlift', date: '2026-01-01', weightKg: 120, reps: 5, sets: 3 }),
    ];
    const a = recommendNextSession(history);
    const b = recommendNextSession(history);
    expect(JSON.stringify(a)).toBe(JSON.stringify(b));
    expect(a.map((r) => r.exerciseName)).toEqual(['Deadlift', 'Squat']);
  });
});
