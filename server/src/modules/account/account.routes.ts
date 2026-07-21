import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { deleteAccount, exportUserData } from './account.service';

export const accountRouter = Router();

/** GDPR-style "download my data". */
accountRouter.get(
  '/me/export',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const data = await exportUserData(req.user!.id);
    res.setHeader('Content-Disposition', 'attachment; filename="nutriai-my-data.json"');
    res.json(data);
  }),
);

/** Delete account + all data (irreversible). */
accountRouter.delete(
  '/me',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    await deleteAccount(req.user!.id);
    res.status(204).end();
  }),
);
