import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { getRiskAssessment } from './risk.service';

export const riskRouter = Router();

// GET /risk?sleepHours=6.5&hydrationPct=40  (the two optional params come from the device).
riskRouter.get(
  '/risk',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const sleepHours = req.query.sleepHours ? Number(req.query.sleepHours) : undefined;
    const hydrationPct = req.query.hydrationPct ? Number(req.query.hydrationPct) : undefined;
    const risk = await getRiskAssessment(req.user!.id, {
      sleepHours: Number.isFinite(sleepHours) ? sleepHours : undefined,
      hydrationPct: Number.isFinite(hydrationPct) ? hydrationPct : undefined,
    });
    res.json({ risk });
  }),
);
