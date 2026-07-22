import { describe, it, expect } from 'vitest';
import { recommendWellness } from '../src/modules/wellness/wellness';

describe('recommendWellness', () => {
  it('always returns a yoga + meditation with a reason', () => {
    const r = recommendWellness({});
    expect(r.yoga).not.toBeNull();
    expect(r.meditation).not.toBeNull();
    expect(r.reason.length).toBeGreaterThan(0);
  });

  it('picks period-relief yoga during the period', () => {
    expect(recommendWellness({ phase: 'menstrual' }).yoga?.id).toBe('period-relief');
  });

  it('picks calming yoga + stress reset when mood is low', () => {
    const r = recommendWellness({ mood: 1 });
    expect(r.yoga?.id).toBe('wind-down');
    expect(r.meditation?.id).toBe('stress-reset');
  });

  it('picks a desk reset for a sedentary lifestyle', () => {
    expect(recommendWellness({ activityLevel: 'sedentary', mood: 3 }).yoga?.id).toBe('desk-reset');
  });

  it('leans energising when mood is high in the follicular phase', () => {
    const r = recommendWellness({ mood: 5, phase: 'follicular' });
    expect(r.yoga?.id).toBe('core-strength');
    expect(r.meditation?.id).toBe('gratitude');
  });
});
