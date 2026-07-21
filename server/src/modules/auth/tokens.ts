import jwt from 'jsonwebtoken';
import { env } from '../../lib/env';

export const ACCESS_TTL_SECONDS = 60 * 60; // 1 hour (client auto-refreshes on 401 anyway)
export const REFRESH_TTL_SECONDS = 30 * 24 * 60 * 60; // 30 days

export interface AccessClaims {
  sub: string; // user id
  role: string;
}

export function signAccessToken(claims: AccessClaims): string {
  return jwt.sign(claims, env.JWT_ACCESS_SECRET, {
    expiresIn: ACCESS_TTL_SECONDS,
    issuer: 'nutriai',
  });
}

export function verifyAccessToken(token: string): AccessClaims {
  const decoded = jwt.verify(token, env.JWT_ACCESS_SECRET, { issuer: 'nutriai' });
  if (typeof decoded === 'string') throw new Error('Invalid token');
  return { sub: String(decoded.sub), role: String((decoded as jwt.JwtPayload).role) };
}

export function refreshExpiryDate(from: Date = new Date()): Date {
  return new Date(from.getTime() + REFRESH_TTL_SECONDS * 1000);
}
