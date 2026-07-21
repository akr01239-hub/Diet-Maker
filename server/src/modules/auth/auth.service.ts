import { prisma } from '../../lib/prisma';
import { randomToken, sha256 } from '../../lib/crypto';
import { HttpError } from '../../middleware/error';
import { hashPassword, verifyPassword } from './password';
import {
  ACCESS_TTL_SECONDS,
  refreshExpiryDate,
  signAccessToken,
} from './tokens';
import type { User } from '@prisma/client';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface RegisterInput {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

function publicUser(u: User) {
  return {
    id: u.id,
    email: u.email,
    firstName: u.firstName,
    lastName: u.lastName,
    role: u.role,
  };
}

async function issueTokens(user: User): Promise<AuthTokens> {
  const accessToken = signAccessToken({ sub: user.id, role: user.role });
  const refreshToken = randomToken();
  await prisma.refreshToken.create({
    data: {
      userId: user.id,
      tokenHash: sha256(refreshToken),
      expiresAt: refreshExpiryDate(),
    },
  });
  return { accessToken, refreshToken, expiresIn: ACCESS_TTL_SECONDS };
}

export async function register(input: RegisterInput) {
  const email = input.email.toLowerCase().trim();
  const existing = await prisma.user.findUnique({ where: { email } });
  if (existing) throw new HttpError(409, 'An account with this email already exists');

  const user = await prisma.user.create({
    data: {
      email,
      passwordHash: await hashPassword(input.password),
      firstName: input.firstName.trim(),
      lastName: input.lastName.trim(),
    },
  });
  await prisma.auditLog.create({
    data: { userId: user.id, action: 'auth.register' },
  });
  return { user: publicUser(user), tokens: await issueTokens(user) };
}

export async function login(email: string, password: string) {
  const user = await prisma.user.findUnique({ where: { email: email.toLowerCase().trim() } });
  // Verify even when the user is missing to reduce timing/enumeration signal.
  const ok = user
    ? await verifyPassword(user.passwordHash, password)
    : await verifyPassword('$argon2id$v=19$m=19456,t=2,p=1$AAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAA', password);
  if (!user || !ok) throw new HttpError(401, 'Invalid email or password');

  await prisma.auditLog.create({ data: { userId: user.id, action: 'auth.login' } });
  return { user: publicUser(user), tokens: await issueTokens(user) };
}

/** Rotates the refresh token: the presented one is revoked and a new pair is issued. */
export async function refresh(rawToken: string): Promise<AuthTokens> {
  const tokenHash = sha256(rawToken);
  const record = await prisma.refreshToken.findUnique({
    where: { tokenHash },
    include: { user: true },
  });
  if (!record || record.revokedAt || record.expiresAt < new Date()) {
    throw new HttpError(401, 'Invalid or expired refresh token');
  }
  await prisma.refreshToken.update({
    where: { id: record.id },
    data: { revokedAt: new Date() },
  });
  return issueTokens(record.user);
}

export async function logout(rawToken: string): Promise<void> {
  const tokenHash = sha256(rawToken);
  await prisma.refreshToken.updateMany({
    where: { tokenHash, revokedAt: null },
    data: { revokedAt: new Date() },
  });
}

export async function getMe(userId: string) {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) throw new HttpError(404, 'User not found');
  return publicUser(user);
}
