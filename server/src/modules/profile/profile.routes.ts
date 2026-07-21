import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { getProfile, upsertProfile } from './profile.service';
import { profileUpsertSchema, calcPreviewSchema } from './profile.schemas';
import { computeCalcResult } from '../nutrition/calcResult';
import { computeAndSaveForUser, latestCalcResult } from '../nutrition/calc.service';

export const profileRouter = Router();

profileRouter.get(
  '/profile',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const profile = await getProfile(req.user!.id);
    res.json({ profile });
  }),
);

profileRouter.put(
  '/profile',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = profileUpsertSchema.parse(req.body);
    const profile = await upsertProfile(req.user!.id, body);
    res.json({ profile });
  }),
);

/** Authenticated: compute + persist the CalcResult from the stored profile. */
profileRouter.post(
  '/calc',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const result = await computeAndSaveForUser(req.user!.id);
    res.json({ result });
  }),
);

profileRouter.get(
  '/calc/latest',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const result = await latestCalcResult(req.user!.id);
    res.json({ result });
  }),
);

/** Unauthenticated onboarding preview — pure calc, nothing stored. */
profileRouter.post(
  '/calc/preview',
  asyncHandler(async (req, res) => {
    const body = calcPreviewSchema.parse(req.body);
    res.json({ result: computeCalcResult(body) });
  }),
);
