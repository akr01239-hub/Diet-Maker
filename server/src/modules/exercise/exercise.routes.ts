import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { prisma } from '../../lib/prisma';
import { decryptJson } from '../../lib/crypto';
import { HttpError } from '../../middleware/error';
import type { SensitiveData } from '../profile/profile.schemas';
import { generateWeeklyWorkout } from './workoutGenerator';
import { localSunday, localToday, tzOffsetMin } from '../../lib/tz';
import type { BodyGoal, ExerciseLocation } from './exercise.types';
import { exerciseLogSchema } from './exerciseLog.schemas';
import * as logSvc from './exerciseLog.service';

export const exerciseRouter = Router();

function dateParam(q: unknown): Date {
  if (typeof q === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(q)) return new Date(`${q}T12:00:00Z`);
  return new Date();
}

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

    const offset = tzOffsetMin(req);
    const plan = generateWeeklyWorkout(goal, location, {
      restDayOfWeek: s.workoutRestDay,
      startDate: localSunday(offset),
      today: localToday(offset),
    });

    res.json({ plan });
  }),
);

// ---- Workout logging (what the user actually performed) ----
exerciseRouter.post(
  '/exercise-logs',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = exerciseLogSchema.parse(req.body);
    const entry = await logSvc.logExercise(req.user!.id, body);
    res.status(201).json({ entry });
  }),
);

exerciseRouter.get(
  '/exercise-logs',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const entries = await logSvc.listExercise(req.user!.id, dateParam(req.query.date), tzOffsetMin(req));
    res.json({ entries });
  }),
);

exerciseRouter.get(
  '/exercise-logs/last',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const last = await logSvc.lastPerformance(req.user!.id);
    res.json({ last });
  }),
);

exerciseRouter.delete(
  '/exercise-logs/:id',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    await logSvc.deleteExercise(req.user!.id, req.params.id!);
    res.status(204).end();
  }),
);
