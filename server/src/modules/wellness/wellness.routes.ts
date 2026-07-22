import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth } from '../../middleware/auth';
import { getWellness } from './wellness';

export const wellnessRouter = Router();

/** Guided yoga flows + meditation/breathing sessions (static, zero-cost). */
wellnessRouter.get(
  '/wellness',
  requireAuth,
  asyncHandler(async (_req, res) => {
    res.json(getWellness());
  }),
);
