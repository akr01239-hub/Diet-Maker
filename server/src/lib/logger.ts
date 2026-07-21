import pino from 'pino';
import { env, isProd } from './env';

/**
 * Structured logger. Pretty output in dev; JSON in prod. Redacts anything that could carry
 * secrets or PII so it never lands in logs.
 */
export const logger = pino({
  level: env.LOG_LEVEL,
  redact: {
    paths: [
      'req.headers.authorization',
      'req.headers.cookie',
      'password',
      'passwordHash',
      '*.password',
      '*.passwordHash',
      'token',
      'accessToken',
      'refreshToken',
    ],
    remove: true,
  },
  transport: isProd
    ? undefined
    : {
        target: 'pino/file', // avoid extra dep; keep plain in dev
        options: { destination: 1 },
      },
});
