import express, { type Express, type Request, type Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { corsOrigins } from './lib/env';
import { errorHandler, notFound } from './middleware/error';
import { healthRouter, API_VERSION } from './modules/health/health.routes';

/**
 * Builds the Express app. Kept free of `listen()` so tests can import it directly
 * (supertest) without binding a port.
 */
export function createApp(): Express {
  const app = express();

  app.disable('x-powered-by');
  app.use(helmet());
  app.use(cors({ origin: corsOrigins, credentials: true }));
  app.use(express.json({ limit: '1mb' }));
  app.use(express.urlencoded({ extended: true }));

  // Liveness/readiness at the root (Render + pingers hit /health).
  app.use('/', healthRouter);

  // Versioned API surface. Real modules mount here from Phase 1 onward.
  const api = express.Router();
  api.get('/', (_req: Request, res: Response) => {
    res.json({ name: 'nutriai-api', version: API_VERSION });
  });
  app.use('/api/v1', api);

  app.use(notFound);
  app.use(errorHandler);

  return app;
}
