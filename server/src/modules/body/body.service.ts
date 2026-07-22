import { prisma } from '../../lib/prisma';
import { requireCompleteProfile } from '../profile/profile.service';
import { ageFromDob } from '../nutrition/calc.service';
import { geminiVisionJson, visionAvailable } from '../../ai/vision';

const DISCLAIMER =
  'This is a rough visual estimate for motivation only — not a medical or DEXA measurement. Body composition is best tracked as a trend over time.';

export interface BodyAssessment {
  available: boolean; // vision provider configured
  refused?: boolean;
  reason?: string;
  bodyFatLow?: number;
  bodyFatHigh?: number;
  category?: string;
  notes?: string;
  confidence?: string;
  formulaEstimatePct?: number | null;
  disclaimer: string;
}

/**
 * Rough body-fat estimate from a user photo via Gemini vision. The image is forwarded to
 * Gemini for this one request and never stored. Refuses for minors and body-image safety.
 */
export async function assessBodyFromPhoto(
  userId: string,
  imageBase64: string,
  mimeType: string,
): Promise<BodyAssessment> {
  const { profile, sensitive } = await requireCompleteProfile(userId);
  const age = ageFromDob(sensitive.dob);

  if (age < 18) {
    return {
      available: true,
      refused: true,
      reason: 'Body-fat photo analysis is disabled for under-18s. Focus on healthy habits, not a number.',
      disclaimer: DISCLAIMER,
    };
  }

  if (!visionAvailable()) {
    return { available: false, disclaimer: DISCLAIMER };
  }

  const snapshot = await prisma.calcResultSnapshot.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  const formulaEstimatePct =
    (snapshot?.result as { bodyFatEstimate?: number } | undefined)?.bodyFatEstimate ?? null;

  const prompt = [
    'You are a supportive fitness assistant giving a rough, educational body-composition estimate from a photo.',
    `Context: ${age}-year-old ${sensitive.sex}, height ${profile.heightCm} cm, weight ${sensitive.currentWeightKg} kg.`,
    formulaEstimatePct != null ? `A formula (BMI-based) estimate is about ${formulaEstimatePct}% body fat.` : '',
    'Give a VISUAL body-fat estimate as a RANGE. Be kind and factual; never comment on attractiveness.',
    'Respond ONLY as JSON with keys: bodyFatLow (number), bodyFatHigh (number), category (one of "lean","fit","average","higher"), notes (1-2 short sentences on visible muscle definition and where fat is stored), confidence ("low" or "medium").',
    'If no clear human body is visible, return bodyFatLow 0, bodyFatHigh 0, notes "No clear body photo detected — try a well-lit, front-facing photo.".',
  ]
    .filter(Boolean)
    .join(' ');

  const result = await geminiVisionJson(imageBase64, mimeType, prompt);
  if (!result) {
    return {
      available: true,
      refused: true,
      reason: 'Could not analyse the photo right now. Please try again with a clear, well-lit, front-facing photo.',
      formulaEstimatePct,
      disclaimer: DISCLAIMER,
    };
  }

  const low = num(result.bodyFatLow);
  const high = num(result.bodyFatHigh);
  if (low === 0 && high === 0) {
    return {
      available: true,
      refused: true,
      reason: 'No clear body photo detected — try a well-lit, front-facing photo.',
      formulaEstimatePct,
      disclaimer: DISCLAIMER,
    };
  }

  return {
    available: true,
    bodyFatLow: low,
    bodyFatHigh: high,
    category: str(result.category),
    notes: str(result.notes),
    confidence: str(result.confidence) || 'low',
    formulaEstimatePct,
    disclaimer: DISCLAIMER,
  };
}

const num = (v: unknown): number => (typeof v === 'number' && isFinite(v) ? Math.round(v * 10) / 10 : 0);
const str = (v: unknown): string => (typeof v === 'string' ? v : '');
