import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { generateAndSavePlan, latestPlan } from './plan.service';
import { tzOffsetMin } from '../../lib/tz';
import { prisma } from '../../lib/prisma';
import { searchUsda, type FoodSearchItem } from './usda';

export const planRouter = Router();

const genSchema = z.object({ days: z.number().int().min(1).max(30).optional() });

/** Generate + persist a new plan from the user's stored profile. */
planRouter.post(
  '/plan',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { days } = genSchema.parse(req.body ?? {});
    const plan = await generateAndSavePlan(req.user!.id, days ?? 7, tzOffsetMin(req));
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

/** Browse the local food database (simple search). */
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

/**
 * Combined food search: local seed foods first, then USDA FoodData Central (when
 * USDA_FDC_API_KEY is set). Every item carries full per-100g macros so it can be logged
 * without a local DB row (USDA foods have no row).
 */
planRouter.get(
  '/foods/search',
  asyncHandler(async (req, res) => {
    const q = typeof req.query.q === 'string' ? req.query.q.trim() : '';

    const local = await prisma.food.findMany({
      where: q ? { name: { contains: q, mode: 'insensitive' } } : undefined,
      orderBy: { name: 'asc' },
      take: 20,
    });

    const localItems: FoodSearchItem[] = local.map((f) => ({
      id: f.id,
      name: f.name,
      kcal: f.kcal,
      proteinG: f.proteinG,
      carbG: f.carbG,
      fatG: f.fatG,
      fiberG: f.fiberG,
      sugarG: f.sugarG,
      sodiumMg: f.sodiumMg,
      typicalServingG: f.typicalServingG,
      source: 'local',
    }));

    // USDA is best-effort: [] when no key or on error.
    const usda = q ? await searchUsda(q) : [];

    res.json({ foods: [...localItems, ...usda] });
  }),
);
