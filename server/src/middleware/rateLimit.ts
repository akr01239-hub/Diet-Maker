import type { NextFunction, Request, Response } from 'express';

interface Bucket {
  count: number;
  resetAt: number;
}

/**
 * Simple fixed-window rate limiter. In-memory (per process) — good enough for the free
 * tier; swap for a Redis store when REDIS_URL is set and horizontal scaling is needed.
 */
export function createRateLimit(opts: { windowMs: number; max: number; keyPrefix?: string }) {
  const buckets = new Map<string, Bucket>();

  // Opportunistic cleanup so the map doesn't grow unbounded.
  function sweep(now: number) {
    if (buckets.size < 5000) return;
    for (const [k, b] of buckets) if (b.resetAt <= now) buckets.delete(k);
  }

  return function rateLimit(req: Request, res: Response, next: NextFunction): void {
    const now = Date.now();
    const ip = req.ip || req.socket.remoteAddress || 'unknown';
    const key = `${opts.keyPrefix ?? ''}${ip}`;

    let b = buckets.get(key);
    if (!b || b.resetAt <= now) {
      b = { count: 0, resetAt: now + opts.windowMs };
      buckets.set(key, b);
    }
    b.count += 1;

    const remaining = Math.max(0, opts.max - b.count);
    res.setHeader('X-RateLimit-Limit', opts.max);
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', Math.ceil(b.resetAt / 1000));

    if (b.count > opts.max) {
      const retryAfter = Math.ceil((b.resetAt - now) / 1000);
      res.setHeader('Retry-After', retryAfter);
      res.status(429).json({ error: 'Too many requests, please slow down', retryAfter });
      return;
    }
    sweep(now);
    next();
  };
}
