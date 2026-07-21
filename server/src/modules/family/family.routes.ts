import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import {
  ACTIVITY_LEVELS,
  DIET_TYPES,
  GOALS,
  sensitiveSchema,
} from '../profile/profile.schemas';
import * as svc from './family.service';

export const familyRouter = Router();

const memberSchema = z.object({
  firstName: z.string().min(1).max(80),
  relation: z.string().max(40).optional(),
  heightCm: z.number().positive().max(272),
  activityLevel: z.enum(ACTIVITY_LEVELS).default('moderate'),
  goal: z.enum(GOALS).default('maintain'),
  dietType: z.enum(DIET_TYPES).default('nonveg'),
  reducedMobility: z.boolean().default(false),
  sensitive: sensitiveSchema,
});

familyRouter.post(
  '/family',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = memberSchema.parse(req.body);
    const member = await svc.addMember(req.user!.id, body);
    res.status(201).json({ member });
  }),
);

familyRouter.get(
  '/family',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const members = await svc.listMembers(req.user!.id);
    res.json({ members });
  }),
);

familyRouter.get(
  '/family/:id/calc',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const result = await svc.memberCalc(req.user!.id, req.params.id!);
    res.json({ result });
  }),
);

familyRouter.post(
  '/family/:id/plan',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const plan = await svc.memberPlan(req.user!.id, req.params.id!, 7);
    res.json({ plan });
  }),
);

familyRouter.delete(
  '/family/:id',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    await svc.removeMember(req.user!.id, req.params.id!);
    res.status(204).end();
  }),
);
