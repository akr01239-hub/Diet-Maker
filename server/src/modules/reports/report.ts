import { round } from '../../calc/anthropometry';

export interface ReportDay {
  date: string; // YYYY-MM-DD
  kcal: number;
  proteinG: number;
}

export interface WeeklyReport {
  name: string;
  generatedAt: string;
  targets: { dailyKcal: number; proteinG: number; waterMl: number } | null;
  bmi: number | null;
  latestWeightKg: number | null;
  weightDeltaKg: number | null;
  days: ReportDay[];
  avgKcal: number | null;
  adherencePct: number | null;
  disclaimer: string;
}

export interface BuildReportInput {
  name: string;
  generatedAt: string; // ISO — passed in so the builder stays pure/deterministic
  targets: { dailyKcal: number; proteinG: number; waterMl: number } | null;
  bmi: number | null;
  latestWeightKg: number | null;
  weightDeltaKg: number | null;
  days: ReportDay[];
}

const DISCLAIMER =
  'Educational guidance, not medical advice. Share with your clinician; do not use for diagnosis.';

export function buildWeeklyReport(input: BuildReportInput): WeeklyReport {
  const avgKcal = input.days.length
    ? round(input.days.reduce((s, d) => s + d.kcal, 0) / input.days.length, 0)
    : null;
  const adherencePct =
    input.targets && avgKcal !== null && input.targets.dailyKcal > 0
      ? round((avgKcal / input.targets.dailyKcal) * 100, 0)
      : null;

  return {
    name: input.name,
    generatedAt: input.generatedAt,
    targets: input.targets,
    bmi: input.bmi,
    latestWeightKg: input.latestWeightKg,
    weightDeltaKg: input.weightDeltaKg,
    days: input.days,
    avgKcal,
    adherencePct,
    disclaimer: DISCLAIMER,
  };
}

const csvCell = (v: string | number | null): string => {
  const s = v === null ? '' : String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

/** Renders the report as CSV (opens directly in Excel/Sheets). */
export function reportToCsv(r: WeeklyReport): string {
  const lines: string[] = [];
  lines.push(csvCell('NutriAI Weekly Report'));
  lines.push([csvCell('Name'), csvCell(r.name)].join(','));
  lines.push([csvCell('Generated'), csvCell(r.generatedAt)].join(','));
  lines.push('');
  lines.push([csvCell('Metric'), csvCell('Value')].join(','));
  lines.push([csvCell('Daily calorie target'), csvCell(r.targets?.dailyKcal ?? '')].join(','));
  lines.push([csvCell('Daily protein target (g)'), csvCell(r.targets?.proteinG ?? '')].join(','));
  lines.push([csvCell('BMI'), csvCell(r.bmi)].join(','));
  lines.push([csvCell('Latest weight (kg)'), csvCell(r.latestWeightKg)].join(','));
  lines.push([csvCell('Weight change (kg)'), csvCell(r.weightDeltaKg)].join(','));
  lines.push([csvCell('Avg calories logged'), csvCell(r.avgKcal)].join(','));
  lines.push([csvCell('Adherence (%)'), csvCell(r.adherencePct)].join(','));
  lines.push('');
  lines.push([csvCell('Date'), csvCell('Calories'), csvCell('Protein (g)')].join(','));
  for (const d of r.days) {
    lines.push([csvCell(d.date), csvCell(d.kcal), csvCell(d.proteinG)].join(','));
  }
  lines.push('');
  lines.push(csvCell(r.disclaimer));
  return lines.join('\n');
}
