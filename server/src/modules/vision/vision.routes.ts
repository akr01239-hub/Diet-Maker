import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { assessBodyFromPhoto } from '../body/body.service';
import { assessMealFromPhoto } from './meal.service';

export const visionRouter = Router();

const photoSchema = z.object({
  imageBase64: z.string().min(100).max(9_000_000),
  mimeType: z.enum(['image/jpeg', 'image/png', 'image/webp']).default('image/jpeg'),
});

/** Rough body-composition estimate from a photo (image forwarded to Gemini, never stored). */
visionRouter.post(
  '/body/assess-photo',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { imageBase64, mimeType } = photoSchema.parse(req.body);
    const assessment = await assessBodyFromPhoto(req.user!.id, imageBase64, mimeType);
    res.json({ assessment });
  }),
);

/** Identify foods + estimate calories from a meal photo, returning loggable items. */
visionRouter.post(
  '/foods/photo',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { imageBase64, mimeType } = photoSchema.parse(req.body);
    const result = await assessMealFromPhoto(imageBase64, mimeType);
    res.json(result);
  }),
);
