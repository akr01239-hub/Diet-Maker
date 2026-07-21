import { Router, type Request, type Response } from 'express';
import { prisma } from '../../lib/prisma';
import { env } from '../../lib/env';

export const API_VERSION = '0.1.0';

/**
 * Liveness + readiness endpoints.
 *
 * - `/health` never touches the DB: it answers "is the process up?" and is what Render's
 *   health check + any uptime pinger hit. Cheap and always 200 when alive.
 * - `/ready` verifies downstream dependencies (DB round-trip) and returns 503 if not ready.
 */
export const healthRouter = Router();

healthRouter.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'ok',
    service: 'nutriai-api',
    version: API_VERSION,
    aiProvider: env.AI_PROVIDER,
    uptimeSeconds: Math.round(process.uptime()),
  });
});

healthRouter.get('/ready', async (_req: Request, res: Response) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.status(200).json({ status: 'ready', db: 'up' });
  } catch {
    // Don't leak connection details.
    res.status(503).json({ status: 'not-ready', db: 'down' });
  }
});
