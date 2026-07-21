import { describe, it, expect } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app';

const app = createApp();

describe('health endpoints', () => {
  it('GET /health returns liveness payload', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
    expect(res.body.service).toBe('nutriai-api');
    expect(res.body.aiProvider).toBe('rules');
    expect(typeof res.body.version).toBe('string');
  });

  it('GET /api/v1 returns the API descriptor', async () => {
    const res = await request(app).get('/api/v1');
    expect(res.status).toBe(200);
    expect(res.body.name).toBe('nutriai-api');
  });

  it('unknown routes return 404 JSON', async () => {
    const res = await request(app).get('/does-not-exist');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('Not Found');
  });
});
