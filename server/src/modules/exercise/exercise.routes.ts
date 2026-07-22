import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { prisma } from '../../lib/prisma';
import { decryptJson } from '../../lib/crypto';
import { HttpError } from '../../middleware/error';
import type { SensitiveData } from '../profile/profile.schemas';
import { generateWeeklyWorkout } from './workoutGenerator';
import type { BodyGoal, ExerciseLocation } from './exercise.types';

export const exerciseRouter = Router();

/** Sensible default body goal from the diet goal when the user hasn't chosen one. */
function goalToBody(goal: string | null | undefined): BodyGoal {
  if (goal === 'gain') return 'muscular';
  if (goal === 'maintain') return 'athletic';
  return 'fatloss';
}

exerciseRouter.get(
  '/exercise-plan',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const profile = await prisma.profile.findUnique({ where: { userId: req.user!.id } });
    if (!profile || !profile.sensitiveEnc) throw new HttpError(400, 'Complete your health profile first');

    const s = decryptJson<SensitiveData>(profile.sensitiveEnc);
    const location: ExerciseLocation = s.exerciseLocation ?? 'home';
    const goal: BodyGoal = s.bodyGoal ?? goalToBody(profile.goal);

    const plan = generateWeeklyWorkout(goal, location, {
      restDayOfWeek: s.workoutRestDay,
      startDate: new Date(),
    });

    res.json({ plan });
  }),
);
