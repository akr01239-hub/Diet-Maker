import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import {
  checkinSchema,
  foodLogSchema,
  waterLogSchema,
} from './logging.schemas';
import * as svc from './logging.service';
import { lookupBarcode } from './barcode';

export const loggingRouter = Router();

function dateParam(q: unknown): Date {
  if (typeof q === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(q)) return new Date(`${q}T12:00:00Z`);
  return new Date();
}

// ---- Food logging ----
loggingRouter.post(
  '/logs/food',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = foodLogSchema.parse(req.body);
    const entry = await svc.logFood(req.user!.id, body);
    res.status(201).json({ entry });
  }),
);

loggingRouter.get(
  '/logs/food',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const entries = await svc.listFood(req.user!.id, dateParam(req.query.date));
    res.json({ entries });
  }),
);

loggingRouter.delete(
  '/logs/food/:id',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    await svc.deleteFood(req.user!.id, req.params.id!);
    res.status(204).end();
  }),
);

// ---- Water ----
loggingRouter.post(
  '/logs/water',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = waterLogSchema.parse(req.body);
    const entry = await svc.logWater(req.user!.id, body);
    res.status(201).json({ entry });
  }),
);

loggingRouter.get(
  '/logs/water',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const totalMl = await svc.waterTotal(req.user!.id, dateParam(req.query.date));
    res.json({ totalMl });
  }),
);

// ---- Weekly check-ins ----
loggingRouter.post(
  '/checkins',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const body = checkinSchema.parse(req.body);
    const checkin = await svc.createCheckin(req.user!.id, body);
    res.status(201).json({ checkin });
  }),
);

loggingRouter.get(
  '/checkins',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const checkins = await svc.listCheckins(req.user!.id);
    res.json({ checkins });
  }),
);

// ---- Dashboard ----
loggingRouter.get(
  '/dashboard',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const dashboard = await svc.getDashboard(req.user!.id);
    res.json({ dashboard });
  }),
);

// ---- Barcode (Open Food Facts, no key) ----
loggingRouter.get(
  '/barcode/:code',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const food = await lookupBarcode(req.params.code!);
    res.json({ food });
  }),
);
