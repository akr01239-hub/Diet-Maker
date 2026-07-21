import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { generateAndSavePlan, latestPlan } from './plan.service';
import { prisma } from '../../lib/prisma';

export const planRouter = Router();

const genSchema = z.object({ days: z.number().int().min(1).max(30).optional() });

/** Generate + persist a new plan from the user's stored profile. */
planRouter.post(
  '/plan',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { days } = genSchema.parse(req.body ?? {});
    const plan = await generateAndSavePlan(req.user!.id, days ?? 7);
    res.status(201).json({ plan });
  }),
);

planRouter.get(
  '/plan/latest',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const plan = await latestPlan(req.user!.id);
    res.json({ plan });
  }),
);

/** Browse the food database (simple search). */
planRouter.get(
  '/foods',
  asyncHandler(async (req, res) => {
    const q = typeof req.query.q === 'string' ? req.query.q : undefined;
    const foods = await prisma.food.findMany({
      where: q ? { name: { contains: q, mode: 'insensitive' } } : undefined,
      orderBy: { name: 'asc' },
      take: 100,
    });
    res.json({ foods });
  }),
);
