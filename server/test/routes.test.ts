import { describe, it, expect } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app';

const app = createApp();

describe('POST /api/v1/calc/preview (no DB, pure calc)', () => {
  it('returns a full CalcResult for a valid body', async () => {
    const res = await request(app)
      .post('/api/v1/calc/preview')
      .send({
        heightCm: 180,
        currentWeightKg: 90,
        targetWeightKg: 78,
        ageYears: 32,
        sex: 'male',
        activityLevel: 'moderate',
        goal: 'lose',
      });
    expect(res.status).toBe(200);
    expect(res.body.result.bmi).toBe(27.8);
    expect(res.body.result.dailyKcal).toBeLessThan(res.body.result.tdee);
    expect(Array.isArray(res.body.result.flags)).toBe(true);
  });

  it('applies guardrails: pregnant preview blocks the deficit', async () => {
    const res = await request(app)
      .post('/api/v1/calc/preview')
      .send({
        heightCm: 165,
        currentWeightKg: 68,
        targetWeightKg: 60,
        ageYears: 29,
        sex: 'female',
        activityLevel: 'light',
        goal: 'lose',
        conditions: ['pregnancy'],
      });
    expect(res.status).toBe(200);
    expect(res.body.result.weightLossBlocked).toBe(true);
    expect(res.body.result.dailyKcal).toBeGreaterThan(res.body.result.tdee);
  });

  it('422s on invalid input', async () => {
    const res = await request(app)
      .post('/api/v1/calc/preview')
      .send({ heightCm: -5, sex: 'other' });
    expect(res.status).toBe(422);
    expect(res.body.error).toBe('Validation failed');
  });
});

describe('auth validation (no DB)', () => {
  it('422s registering with a short password', async () => {
    const res = await request(app)
      .post('/api/v1/auth/register')
      .send({ email: 'a@b.com', password: 'short', firstName: 'A', lastName: 'B' });
    expect(res.status).toBe(422);
  });

  it('401s on /auth/me without a token', async () => {
    const res = await request(app).get('/api/v1/auth/me');
    expect(res.status).toBe(401);
  });
});
