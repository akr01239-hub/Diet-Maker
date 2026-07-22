import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import * as svc from './cycle.service';

export const cycleRouter = Router();

const periodSchema = z.object({
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional(),
});

/** Log a period start (defaults to now) and optional end. */
cycleRouter.post(
  '/cycle/period',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { startDate, endDate } = periodSchema.parse(req.body ?? {});
    const row = await svc.logPeriod(
      req.user!.id,
      startDate ? new Date(startDate) : new Date(),
      endDate ? new Date(endDate) : undefined,
    );
    res.status(201).json({ period: { id: row.id, startDate: row.startDate, endDate: row.endDate } });
  }),
);

/** Mark the current (ongoing) period as ended today — enables duration analysis. */
cycleRouter.post(
  '/cycle/end',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const row = await svc.endLatestPeriod(req.user!.id);
    res.json({ period: { id: row.id, startDate: row.startDate, endDate: row.endDate } });
  }),
);

/** Current cycle phase + phase-specific diet/exercise/yoga guidance. */
cycleRouter.get(
  '/cycle',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const cycle = await svc.getCycle(req.user!.id);
    res.json({ cycle });
  }),
);
