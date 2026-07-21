import { describe, it, expect, vi } from 'vitest';
import request from 'supertest';
import type { Request, Response } from 'express';
import { createApp } from '../src/app';
import { createRateLimit } from '../src/middleware/rateLimit';

const app = createApp();

function mockReqRes(ip: string) {
  const req = { ip, socket: { remoteAddress: ip }, header: () => undefined } as unknown as Request;
  const headers: Record<string, unknown> = {};
  let status = 200;
  const res = {
    setHeader: (k: string, v: unknown) => {
      headers[k] = v;
    },
    status: (s: number) => {
      status = s;
      return res;
    },
    json: () => res,
  } as unknown as Response;
  return { req, res, getStatus: () => status };
}

describe('security middleware', () => {
  it('sets a correlation id header', async () => {
    const res = await request(app).get('/health');
    expect(res.headers['x-request-id']).toBeDefined();
  });

  it('echoes a provided request id', async () => {
    const res = await request(app).get('/health').set('x-request-id', 'abc-123');
    expect(res.headers['x-request-id']).toBe('abc-123');
  });

  it('exposes rate-limit headers on API routes', async () => {
    const res = await request(app).get('/api/v1');
    expect(res.headers['x-ratelimit-limit']).toBeDefined();
    expect(res.headers['x-ratelimit-remaining']).toBeDefined();
  });

  it('sets helmet security headers', async () => {
    const res = await request(app).get('/health');
    // helmet sets these by default
    expect(res.headers['x-content-type-options']).toBe('nosniff');
    expect(res.headers['x-powered-by']).toBeUndefined();
  });

  it('429s once the window max is exceeded (isolated middleware)', () => {
    const limiter = createRateLimit({ windowMs: 60_000, max: 3 });
    const next = vi.fn();
    for (let i = 0; i < 3; i++) {
      const { req, res, getStatus } = mockReqRes('1.2.3.4');
      limiter(req, res, next);
      expect(getStatus()).toBe(200);
    }
    const { req, res, getStatus } = mockReqRes('1.2.3.4');
    limiter(req, res, next);
    expect(getStatus()).toBe(429);
    expect(next).toHaveBeenCalledTimes(3); // 4th blocked
  });

  it('tracks limits per IP independently', () => {
    const limiter = createRateLimit({ windowMs: 60_000, max: 1 });
    const next = vi.fn();
    const a = mockReqRes('10.0.0.1');
    const b = mockReqRes('10.0.0.2');
    limiter(a.req, a.res, next);
    limiter(b.req, b.res, next);
    expect(a.getStatus()).toBe(200);
    expect(b.getStatus()).toBe(200);
  });
});
