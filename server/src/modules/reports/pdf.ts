import PDFDocument from 'pdfkit';
import type { WeeklyReport } from './report';

/** Renders a WeeklyReport to a PDF Buffer using pdfkit (pure JS, no native deps). */
export function renderReportPdf(r: WeeklyReport): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 50 });
    const chunks: Buffer[] = [];
    doc.on('data', (c: Buffer) => chunks.push(c));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);

    const green = '#0E7C3A';

    doc.fillColor(green).fontSize(22).text('NutriAI', { continued: true });
    doc.fillColor('#555').fontSize(12).text('  Weekly Report');
    doc.moveDown(0.5);
    doc.fillColor('#000').fontSize(11);
    doc.text(`Name: ${r.name}`);
    doc.text(`Generated: ${r.generatedAt}`);
    doc.moveDown();

    const line = (label: string, value: string | number | null) =>
      doc.font('Helvetica-Bold').text(`${label}: `, { continued: true }).font('Helvetica').text(value === null ? '—' : String(value));

    doc.fillColor(green).fontSize(14).text('Summary');
    doc.fillColor('#000').fontSize(11).moveDown(0.3);
    line('Daily calorie target', r.targets?.dailyKcal ?? null);
    line('Daily protein target (g)', r.targets?.proteinG ?? null);
    line('BMI', r.bmi);
    line('Latest weight (kg)', r.latestWeightKg);
    line('Weight change (kg)', r.weightDeltaKg);
    line('Avg calories logged', r.avgKcal);
    line('Adherence (%)', r.adherencePct);
    doc.moveDown();

    doc.fillColor(green).fontSize(14).text('Daily log (last 7 days)');
    doc.fillColor('#000').fontSize(11).moveDown(0.3);
    doc.font('Helvetica-Bold').text('Date', 50, doc.y, { continued: true, width: 180 });
    doc.text('Calories', { continued: true, width: 120 }).text('Protein (g)');
    doc.font('Helvetica');
    if (r.days.length === 0) {
      doc.fillColor('#777').text('No entries logged yet.');
      doc.fillColor('#000');
    } else {
      for (const d of r.days) {
        doc.text(d.date, 50, doc.y, { continued: true, width: 180 });
        doc.text(String(d.kcal), { continued: true, width: 120 }).text(String(d.proteinG));
      }
    }
    doc.moveDown(2);

    doc.fillColor('#777').fontSize(9).text(r.disclaimer, { align: 'center' });

    doc.end();
  });
}
