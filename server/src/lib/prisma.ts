import { PrismaClient } from '@prisma/client';
import { env } from './env';

/**
 * Single shared PrismaClient. In dev we stash it on globalThis so hot-reload (tsx watch)
 * doesn't exhaust the connection pool by creating a new client on every reload.
 */
const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient };

export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    log: env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error'],
  });

if (env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma;
}
