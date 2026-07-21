import type { NextFunction, Request, Response, RequestHandler } from 'express';

/** Wraps an async handler so rejected promises reach Express's error middleware. */
export function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<unknown>,
): RequestHandler {
  return (req, res, next) => {
    fn(req, res, next).catch(next);
  };
}
