import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { requireCompleteProfile } from '../profile/profile.service';
import { ageFromDob } from '../nutrition/calc.service';
import { buildGuidance } from './guidance';
import type { Goal } from '../../calc/types';

export const guidanceRouter = Router();

/** Personalized diet + exercise guidance from the user's conditions, sex and lifestyle. */
guidanceRouter.get(
  '/guidance',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { profile, sensitive } = await requireCompleteProfile(req.user!.id);
    const guidance = buildGuidance({
      sex: sensitive.sex,
      ageYears: ageFromDob(sensitive.dob),
      goal: profile.goal as Goal,
      activityLevel: profile.activityLevel,
      conditions: sensitive.conditions ?? [],
      bodyGoal: sensitive.bodyGoal,
    });
    res.json({ guidance });
  }),
);
