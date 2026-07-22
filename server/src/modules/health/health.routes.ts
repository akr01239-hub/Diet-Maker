import { Router, type Request, type Response } from 'express';
import { prisma } from '../../lib/prisma';
import { env } from '../../lib/env';
import { geminiTextJson, groqTextJson } from '../../ai/vision';

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

/**
 * AI diagnostic: reports which LLM keys are configured and whether a live test call to each
 * actually succeeds. Never reveals key values. Open in a browser to debug "AI not working".
 */
healthRouter.get('/ai-status', async (_req: Request, res: Response) => {
  const result = {
    aiProvider: env.AI_PROVIDER,
    gemini: { configured: Boolean(env.GEMINI_API_KEY), working: null as boolean | null },
    groq: { configured: Boolean(env.GROQ_API_KEY), working: null as boolean | null },
    note: '"working: true" means a live test call succeeded. Vision (photo) features require Gemini.',
  };
  if (env.GEMINI_API_KEY) {
    result.gemini.working = (await geminiTextJson('Return this exact JSON: {"ok": true}')) !== null;
  }
  if (env.GROQ_API_KEY) {
    result.groq.working = (await groqTextJson('Return this exact JSON: {"ok": true}')) !== null;
  }
  res.status(200).json(result);
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
