import { Router } from 'express';
import { z } from 'zod';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { chat } from './chat.service';
import { getAdaptation, defaultReminders } from './adaptive.service';
import { getMe } from '../auth/auth.service';

export const chatRouter = Router();

const chatSchema = z.object({ message: z.string().min(1).max(1000) });

chatRouter.post(
  '/chat',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const { message } = chatSchema.parse(req.body);
    const me = await getMe(req.user!.id);
    const reply = await chat(req.user!.id, message, me.firstName);
    res.json({ reply });
  }),
);

chatRouter.get(
  '/adapt',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const adaptation = await getAdaptation(req.user!.id);
    res.json({ adaptation });
  }),
);

chatRouter.get(
  '/reminders',
  requireAuth,
  asyncHandler(async (_req: AuthedRequest, res) => {
    res.json({ reminders: defaultReminders() });
  }),
);
