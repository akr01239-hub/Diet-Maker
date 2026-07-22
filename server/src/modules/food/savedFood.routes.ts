import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { savedFoodSchema, createSaved, listSaved, deleteSaved, recentFoods } from './savedFood.service';

export const savedFoodRouter = Router();

/** Save a favourite / custom food (per-100g). */
savedFoodRouter.post(
  '/foods/saved',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = savedFoodSchema.parse(req.body);
    const food = await createSaved(req.user!.id, body);
    res.status(201).json({ food });
  }),
);

savedFoodRouter.get(
  '/foods/saved',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const foods = await listSaved(req.user!.id);
    res.json({ foods });
  }),
);

savedFoodRouter.delete(
  '/foods/saved/:id',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    await deleteSaved(req.user!.id, req.params.id!);
    res.status(204).end();
  }),
);

/** Recently-logged foods for one-tap re-logging. */
savedFoodRouter.get(
  '/foods/recent',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const foods = await recentFoods(req.user!.id);
    res.json({ foods });
  }),
);
