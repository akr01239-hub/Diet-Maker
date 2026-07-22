import { prisma } from '../../lib/prisma';
import { requireCompleteProfile } from '../profile/profile.service';
import { ageFromDob } from '../nutrition/calc.service';
import { geminiVisionJson, visionAvailable } from '../../ai/vision';
import { bmi as bmiCalc, bodyFatDeurenberg } from '../../calc/anthropometry';

const DISCLAIMER =
  'This is a rough estimate for motivation only — not a medical or DEXA measurement. Body composition is best tracked as a trend over time.';

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
  /** Where the estimate came from: 'ai' (photo) or 'formula' (BMI/age/sex fallback). */
  source?: 'ai' | 'formula';
  disclaimer: string;
}

/** Body-fat category from % and sex (ACE-style bands). */
function bfCategory(pct: number, sex: string): string {
  const male = sex === 'male';
  if (pct < (male ? 11 : 16)) return 'lean';
  if (pct < (male ? 18 : 24)) return 'fit';
  if (pct < (male ? 25 : 32)) return 'average';
  return 'higher';
}

/**
 * Deterministic body-fat estimate from height/weight/age/sex (Deurenberg). Always available,
 * zero-cost — used as the fallback whenever photo AI is unavailable or can't read the image.
 */
function formulaAssessment(
  bfPct: number,
  sex: string,
  fromPhotoAttempt: boolean,
): BodyAssessment {
  const low = Math.max(3, Math.round((bfPct - 3.5) * 10) / 10);
  const high = Math.round((bfPct + 3.5) * 10) / 10;
  return {
    available: true,
    bodyFatLow: low,
    bodyFatHigh: high,
    category: bfCategory(bfPct, sex),
    notes: fromPhotoAttempt
      ? 'Couldn’t read the photo clearly, so this is estimated from your height, weight, age and sex. For a photo-based read, try a well-lit, front-facing shot.'
      : 'Estimated from your height, weight, age and sex. Track the trend as your weight changes.',
    confidence: 'low',
    formulaEstimatePct: bfPct,
    source: 'formula',
    disclaimer: DISCLAIMER,
  };
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

  const snapshot = await prisma.calcResultSnapshot.findFirst({
    where: { userId },
    orderBy: { createdAt: 'desc' },
  });
  // Deterministic body-fat estimate (Deurenberg) — always computable, and the fallback when
  // photo AI is down. Prefer the persisted snapshot; else compute from current stats.
  const formulaEstimatePct =
    (snapshot?.result as { bodyFatEstimate?: number } | undefined)?.bodyFatEstimate ??
    bodyFatDeurenberg({
      bmi: bmiCalc(sensitive.currentWeightKg, profile.heightCm),
      ageYears: age,
      sex: sensitive.sex,
    });

  // No photo AI configured / quota exhausted → still give a real number from the formula.
  if (!visionAvailable()) {
    return formulaAssessment(formulaEstimatePct, sensitive.sex, false);
  }

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
  // Vision failed (quota/error) or couldn't see a body → fall back to the formula estimate
  // so the user still gets a useful number instead of a dead-end error.
  if (!result) {
    return formulaAssessment(formulaEstimatePct, sensitive.sex, true);
  }

  const low = num(result.bodyFatLow);
  const high = num(result.bodyFatHigh);
  if (low === 0 && high === 0) {
    return formulaAssessment(formulaEstimatePct, sensitive.sex, true);
  }

  return {
    available: true,
    bodyFatLow: low,
    bodyFatHigh: high,
    category: str(result.category),
    notes: str(result.notes),
    confidence: str(result.confidence) || 'low',
    formulaEstimatePct,
    source: 'ai',
    disclaimer: DISCLAIMER,
  };
}

const num = (v: unknown): number => (typeof v === 'number' && isFinite(v) ? Math.round(v * 10) / 10 : 0);
const str = (v: unknown): string => (typeof v === 'string' ? v : '');
