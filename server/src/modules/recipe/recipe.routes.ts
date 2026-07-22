import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth } from '../../middleware/auth';
import { getRecipe } from './recipe.service';

export const recipeRouter = Router();

/** Cooking recipe for a planned dish. ?food=<name>&foodId=<id> */
recipeRouter.get(
  '/recipe',
  requireAuth,
  asyncHandler(async (req, res) => {
    const food = typeof req.query.food === 'string' ? req.query.food.trim() : '';
    const foodId = typeof req.query.foodId === 'string' ? req.query.foodId : undefined;
    if (!food) {
      res.status(400).json({ error: 'food query param required' });
      return;
    }
    const recipe = await getRecipe(food, foodId);
    res.json({ recipe });
  }),
);
