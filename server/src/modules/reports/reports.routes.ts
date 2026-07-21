import { Router } from 'express';
import { asyncHandler } from '../../lib/asyncHandler';
import { requireAuth, type AuthedRequest } from '../../middleware/auth';
import { getGrocery, getWeeklyReport, getGamification } from './reports.service';
import { reportToCsv } from './report';
import { renderReportPdf } from './pdf';

export const reportsRouter = Router();

reportsRouter.get(
  '/grocery',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const grocery = await getGrocery(req.user!.id);
    res.json({ grocery });
  }),
);

reportsRouter.get(
  '/gamification',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const gamification = await getGamification(req.user!.id);
    res.json({ gamification });
  }),
);

reportsRouter.get(
  '/reports/weekly.json',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const report = await getWeeklyReport(req.user!.id, new Date().toISOString());
    res.json({ report });
  }),
);

reportsRouter.get(
  '/reports/weekly.csv',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const report = await getWeeklyReport(req.user!.id, new Date().toISOString());
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="nutriai-weekly.csv"');
    res.send(reportToCsv(report));
  }),
);

reportsRouter.get(
  '/reports/weekly.pdf',
  requireAuth,
  asyncHandler(async (req: AuthedRequest, res) => {
    const report = await getWeeklyReport(req.user!.id, new Date().toISOString());
    const pdf = await renderReportPdf(report);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'attachment; filename="nutriai-weekly.pdf"');
    res.send(pdf);
  }),
);
