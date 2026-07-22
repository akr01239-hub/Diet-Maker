/**
 * Menstrual-cycle phase engine. Pure and deterministic. Given the last period start and
 * today, it estimates the current phase and returns phase-specific diet, exercise and yoga
 * guidance. Educational only — not a contraceptive or diagnostic tool.
 */

export type CyclePhase = 'menstrual' | 'follicular' | 'ovulation' | 'luteal';

export interface CycleState {
  cycleDay: number;
  phase: CyclePhase;
  phaseLabel: string;
  nextPeriodInDays: number;
  shouldPromptPeriod: boolean;
}

export interface CycleGuidance {
  summary: string;
  dietTips: string[];
  exerciseTips: string[];
  yogaTips: string[];
  /** Smart swaps for the cravings that spike before and during the period. */
  cravingTips: string[];
}

/** Craving-buster swaps — shared by the phases where PMS/period cravings hit hardest. */
const CRAVING_TIPS: string[] = [
  'Craving chocolate? It is often a magnesium dip — have 1–2 squares of 70%+ dark chocolate, or cocoa in warm milk, with a few nuts.',
  'Craving sweets? Reach for fruit (banana, dates, berries), curd with honey, or dates stuffed with peanut butter before packaged sugar.',
  'Craving salty & crunchy? Roasted chana, makhana, air-popped popcorn, roasted seeds or cucumber with chaat masala beat chips.',
  'Craving carbs? Your body wants serotonin — whole-grain toast, oats or sweet potato steady mood far better than maida/sugar.',
  'Prevent the crash: eat protein + fibre at regular times so blood sugar never dips low enough to trigger a binge. Water first — thirst mimics hunger.',
  'Do not over-restrict — one planned treat prevents an unplanned binge. Magnesium (pumpkin seeds, dark chocolate, banana) helps too.',
];

const DAY_MS = 86_400_000;

function daysBetween(a: Date, b: Date): number {
  const ua = Date.UTC(a.getUTCFullYear(), a.getUTCMonth(), a.getUTCDate());
  const ub = Date.UTC(b.getUTCFullYear(), b.getUTCMonth(), b.getUTCDate());
  return Math.round((ub - ua) / DAY_MS);
}

/** Estimate the current cycle day + phase from the last period start. */
export function computeCycle(
  lastStart: Date,
  today: Date,
  cycleLengthDays = 28,
  periodLengthDays = 5,
): CycleState {
  const cycleLen = Math.min(45, Math.max(21, Math.round(cycleLengthDays)));
  const periodLen = Math.min(10, Math.max(2, Math.round(periodLengthDays)));
  const daysSince = Math.max(0, daysBetween(lastStart, today));
  const cycleDay = daysSince + 1;

  // Luteal phase is ~14 days, so ovulation ≈ cycleLen − 14.
  const ovulation = Math.max(periodLen + 2, cycleLen - 14);

  let phase: CyclePhase;
  if (cycleDay <= periodLen) phase = 'menstrual';
  else if (cycleDay < ovulation - 1) phase = 'follicular';
  else if (cycleDay <= ovulation + 1) phase = 'ovulation';
  else phase = 'luteal';

  const nextPeriodInDays = Math.max(0, cycleLen - daysSince);
  // Ask "has your period started?" from ~day 25 (user-configurable window), or when overdue.
  const promptFrom = Math.min(25, cycleLen - 3);
  const shouldPromptPeriod = cycleDay >= promptFrom;

  return { cycleDay, phase, phaseLabel: PHASE_LABEL[phase], nextPeriodInDays, shouldPromptPeriod };
}

const PHASE_LABEL: Record<CyclePhase, string> = {
  menstrual: 'Menstrual (period)',
  follicular: 'Follicular (post-period)',
  ovulation: 'Ovulation (peak energy)',
  luteal: 'Luteal (pre-period)',
};

const GUIDANCE: Record<CyclePhase, CycleGuidance> = {
  menstrual: {
    summary: 'Period: replenish iron, ease cramps, and move gently.',
    dietTips: [
      'Replace lost iron: leafy greens, dates, jaggery, legumes, and (if non-veg) red meat or liver.',
      'Pair iron with vitamin C (lemon, amla, citrus, tomato) to absorb it better.',
      'Magnesium eases cramps — dark chocolate, banana, nuts, seeds; ginger/turmeric are anti-inflammatory.',
      'Favour warm, cooked, easy-to-digest meals; cut excess salt and caffeine to reduce bloating.',
    ],
    exerciseTips: [
      'Energy is naturally low — keep it gentle: walking, light yoga, stretching, mobility.',
      'Skip max-effort sessions; if you feel good, light strength is fine. Rest is productive here.',
    ],
    yogaTips: [
      "Restorative & cramp-relief: Child's pose, Cat-Cow, Reclining bound angle, Supine twist, Legs-up-the-wall.",
      'Avoid strong inversions and intense core work; breathe slow and long.',
    ],
    cravingTips: CRAVING_TIPS,
  },
  follicular: {
    summary: 'Post-period: energy climbing — the best window to push.',
    dietTips: [
      'Rising energy — lean protein, complex carbs and fresh produce fuel training well.',
      'Add fermented foods (curd, idli, kimchi) and light, fibre-rich meals.',
      'A great phase to lean into your fat-loss or performance goal if you have one.',
    ],
    exerciseTips: [
      'Estrogen and energy are rising — ideal for strength training, HIIT and trying harder/new workouts.',
      'Push progressive overload now: add reps or load while recovery is strong.',
    ],
    yogaTips: ['Energising flow: Sun Salutations, Warrior I/II, Triangle, gentle backbends.'],
    cravingTips: [],
  },
  ovulation: {
    summary: 'Ovulation: peak strength — go for your best efforts.',
    dietTips: [
      'Peak energy — antioxidants (berries, colourful veg), fibre and anti-inflammatory foods.',
      'Support with magnesium and zinc (seeds, nuts, whole grains).',
    ],
    exerciseTips: [
      'Peak strength and power — a great time for PRs, heavy lifts and hard HIIT.',
      'Warm up thoroughly: joints are a little laxer mid-cycle, so protect your knees on jumps/heavy squats.',
    ],
    yogaTips: ['Dynamic, heat-building flow: Sun Salutation B, standing balances, twists.'],
    cravingTips: [],
  },
  luteal: {
    summary: 'Pre-period: steady the mood, ease off intensity toward the end.',
    dietTips: [
      'Cravings & PMS are hormonal — complex carbs (whole grains, sweet potato, oats) steady mood better than sugar.',
      'Magnesium + calcium (greens, dairy/fortified, seeds) and vitamin B6 may reduce PMS symptoms.',
      'Cut back on salt, sugar and caffeine late in this phase to limit bloating and mood swings.',
      'A slightly higher appetite is normal — metabolism ticks up; eat to satisfaction with protein + fibre.',
    ],
    exerciseTips: [
      'Energy dips as your period nears — moderate strength and steady cardio early, easing to walking/yoga later.',
      "Don't judge yourself on lower performance now; it's physiology, not lost progress.",
    ],
    yogaTips: [
      'Calming & grounding: forward folds, hip openers (Pigeon, Garland), gentle twists; go restorative as the period nears.',
    ],
    cravingTips: CRAVING_TIPS,
  },
};

export function cycleGuidance(phase: CyclePhase): CycleGuidance {
  return GUIDANCE[phase];
}
