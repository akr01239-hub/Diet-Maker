import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { getWellness, recommendWellness } from './wellness';
import { requireCompleteProfile } from '../profile/profile.service';
import { getCycle } from '../cycle/cycle.service';

export const wellnessRouter = Router();

/** Guided yoga flows + meditation/breathing sessions (static, zero-cost). */
wellnessRouter.get(
  '/wellness',
  requireAuth,
  asyncHandler(async (_req, res) => {
    res.json(getWellness());
  }),
);

/**
 * The day's recommended yoga + meditation, chosen from the whole picture: cycle phase,
 * today's mood (?mood=1..5) and lifestyle/conditions. ?mood is optional.
 */
wellnessRouter.get(
  '/wellness/recommend',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { profile, sensitive } = await requireCompleteProfile(req.user!.id);
    const moodRaw = Number(req.query.mood);
    const mood = Number.isFinite(moodRaw) && moodRaw >= 1 && moodRaw <= 5 ? moodRaw : null;

    let phase: string | null = null;
    if (sensitive.sex === 'female') {
      const cycle = await getCycle(req.user!.id);
      if ('phase' in cycle && cycle.phase) phase = cycle.phase;
    }

    const recommendation = recommendWellness({
      phase,
      mood,
      conditions: sensitive.conditions ?? [],
      activityLevel: profile.activityLevel,
    });
    res.json({ recommendation });
  }),
);
