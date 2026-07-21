import type { NextFunction, Request, Response } from 'express';
import { HttpError } from './error';
import { verifyAccessToken } from '../modules/auth/tokens';

export interface AuthedRequest extends Request {
  user?: { id: string; role: string };
}

/** Requires a valid Bearer access token; attaches req.user. */
export function requireAuth(req: AuthedRequest, _res: Response, next: NextFunction): void {
  const header = req.header('authorization');
  if (!header?.startsWith('Bearer ')) {
    throw new HttpError(401, 'Missing bearer token');
  }
  try {
    const claims = verifyAccessToken(header.slice('Bearer '.length));
    req.user = { id: claims.sub, role: claims.role };
    next();
  } catch {
    throw new HttpError(401, 'Invalid or expired token');
  }
}

/** Requires the authenticated user to hold one of the given roles. */
export function requireRole(...roles: string[]) {
  return (req: AuthedRequest, _res: Response, next: NextFunction): void => {
    if (!req.user) throw new HttpError(401, 'Not authenticated');
    if (!roles.includes(req.user.role)) throw new HttpError(403, 'Forbidden');
    next();
  };
}
