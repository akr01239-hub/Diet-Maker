import { round } from '../../calc/anthropometry';

export interface ReportDay {
  date: string; // YYYY-MM-DD
  kcal: number;
  proteinG: number;
}

/** A single itemised food-log entry for the detailed table. */
export interface FoodEntry {
  date: string; // YYYY-MM-DD
  mealSlot: string;
  name: string;
  grams: number;
  kcal: number;
  proteinG: number;
}

/** Water intake for a single day. */
export interface WaterDay {
  date: string;
  ml: number;
}

/** Lifetime totals so the report doubles as an all-time record. */
export interface AllTimeStats {
  daysLogged: number;
  totalKcal: number;
  avgDailyKcal: number;
  totalWaterMl: number;
}

export interface WeeklyReport {
  name: string;
  generatedAt: string;
  targets: { dailyKcal: number; proteinG: number; waterMl: number } | null;
  bmi: number | null;
  latestWeightKg: number | null;
  weightDeltaKg: number | null;
  days: ReportDay[];
  entries: FoodEntry[];
  waterByDay: WaterDay[];
  allTime: AllTimeStats | null;
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
  entries?: FoodEntry[];
  waterByDay?: WaterDay[];
  allTime?: AllTimeStats | null;
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
    entries: input.entries ?? [],
    waterByDay: input.waterByDay ?? [],
    allTime: input.allTime ?? null,
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
  // Itemised food log.
  if (r.entries.length) {
    lines.push('');
    lines.push(csvCell('What you ate'));
    lines.push([csvCell('Date'), csvCell('Meal'), csvCell('Item'), csvCell('Qty (g)'), csvCell('Calories'), csvCell('Protein (g)')].join(','));
    for (const e of r.entries) {
      lines.push([csvCell(e.date), csvCell(e.mealSlot), csvCell(e.name), csvCell(e.grams), csvCell(e.kcal), csvCell(e.proteinG)].join(','));
    }
  }
  // Water log.
  if (r.waterByDay.length) {
    lines.push('');
    lines.push(csvCell('Water intake'));
    lines.push([csvCell('Date'), csvCell('Water (ml)')].join(','));
    for (const w of r.waterByDay) lines.push([csvCell(w.date), csvCell(w.ml)].join(','));
  }
  if (r.allTime) {
    lines.push('');
    lines.push(csvCell('All-time record'));
    lines.push([csvCell('Days logged'), csvCell(r.allTime.daysLogged)].join(','));
    lines.push([csvCell('Total calories consumed'), csvCell(r.allTime.totalKcal)].join(','));
    lines.push([csvCell('Average daily calories'), csvCell(r.allTime.avgDailyKcal)].join(','));
    lines.push([csvCell('Total water (ml)'), csvCell(r.allTime.totalWaterMl)].join(','));
  }
  lines.push('');
  lines.push(csvCell(r.disclaimer));
  return lines.join('\n');
}
