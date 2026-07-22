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
  /** Premenstrual-syndrome (PMS) management — shown in the pre-period (luteal) phase. */
  pmsTips: string[];
}

/** PMS ruleset — evidence-based management for the luteal (pre-period) phase. */
const PMS_TIPS: string[] = [
  'PMS in the week or so before your period is normal — mood swings, bloating, cramps, tender breasts, fatigue and irritability come from the natural hormone drop.',
  'Calcium + vitamin D (dairy/fortified foods, leafy greens, sunlight) and magnesium (nuts, seeds, dark chocolate) are the best-evidenced nutrients for easing PMS.',
  'Vitamin B6 (banana, potato, chickpeas, fish) can help with mood and irritability.',
  'Cut back on salt (bloating), sugar and caffeine (mood/anxiety) and alcohol this week.',
  'Regular aerobic exercise, 7–9h sleep and stress relief (yoga, slow breathing) measurably reduce PMS.',
  'If symptoms are severe enough to disrupt your life (intense depression, anxiety or anger), that may be PMDD — please see a doctor.',
];

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
    pmsTips: [],
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
    pmsTips: [],
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
    pmsTips: [],
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
    pmsTips: PMS_TIPS,
  },
};

export function cycleGuidance(phase: CyclePhase): CycleGuidance {
  return GUIDANCE[phase];
}

// ---- Menstrual-health analysis (educational, non-diagnostic) ----

export interface CycleFinding {
  issue: string;
  possibleCauses: string;
  severity: 'watch' | 'concern';
}

export interface CycleHealth {
  status: 'insufficient_data' | 'ok' | 'watch' | 'concern';
  headline: string;
  findings: CycleFinding[];
  advice: { diet: string[]; sleep: string[]; lifestyle: string[]; exercise: string[] };
  seeDoctor: boolean;
  disclaimer: string;
}

export interface CycleHealthInput {
  cycleLengths: number[]; // gaps (days) between consecutive period starts
  periodLengths: number[]; // durations (days) of completed periods
  daysSinceLastStart: number;
  avgCycleLength: number;
  smoking?: string; // 'no' | 'occasional' | 'regular'
  alcohol?: string;
}

const HEALTH_DISCLAIMER =
  'Educational only — not a diagnosis. See a gynaecologist for periods that are very short/long, painful, heavy, irregular or missed, especially with other symptoms.';

/** Flags common menstrual-health concerns and returns doctor-style (educational) advice. */
export function analyzeCycleHealth(input: CycleHealthInput): CycleHealth {
  const { cycleLengths, periodLengths, daysSinceLastStart, avgCycleLength, smoking, alcohol } = input;
  const findings: CycleFinding[] = [];

  const enoughCycles = cycleLengths.length >= 2;
  const haveDuration = periodLengths.length >= 1;

  if (!enoughCycles && !haveDuration) {
    return {
      status: 'insufficient_data',
      headline: 'Log a couple of full periods (start + end) so I can review your cycle health.',
      findings: [],
      advice: baseAdvice(smoking, alcohol),
      seeDoctor: false,
      disclaimer: HEALTH_DISCLAIMER,
    };
  }

  // Period duration
  if (haveDuration) {
    const minLen = Math.min(...periodLengths);
    const maxLen = Math.max(...periodLengths);
    if (minLen <= 2) {
      findings.push({
        issue: `Very short period (${minLen} day${minLen === 1 ? '' : 's'})`,
        possibleCauses:
          'Low estrogen, high stress, rapid weight loss or very low body fat, over-exercising, PCOS, thyroid issues, or approaching menopause. Some spotting can also be mistaken for a period.',
        severity: 'concern',
      });
    }
    if (maxLen >= 8) {
      findings.push({
        issue: `Long period (${maxLen} days)`,
        possibleCauses:
          'Hormonal imbalance, fibroids or polyps, thyroid issues, or a bleeding disorder. Long/heavy bleeding also risks iron-deficiency anaemia.',
        severity: 'concern',
      });
    }
  }

  // Cycle length + regularity
  if (enoughCycles) {
    const spread = Math.max(...cycleLengths) - Math.min(...cycleLengths);
    if (spread > 9) {
      findings.push({
        issue: `Irregular cycle length (varies by ${spread} days)`,
        possibleCauses:
          'PCOS, thyroid imbalance, high stress, significant weight change, or intense over-training are the usual drivers of irregular cycles.',
        severity: 'concern',
      });
    }
    if (avgCycleLength < 21) {
      findings.push({
        issue: `Short cycles (avg ${avgCycleLength} days)`,
        possibleCauses: 'A short luteal phase or hormonal imbalance; sometimes thyroid or approaching perimenopause.',
        severity: 'watch',
      });
    }
    if (avgCycleLength > 35) {
      findings.push({
        issue: `Long cycles (avg ${avgCycleLength} days)`,
        possibleCauses: 'Infrequent ovulation — commonly PCOS or thyroid, sometimes high stress or low body weight.',
        severity: 'concern',
      });
    }
  }

  // Overdue / possibly missed
  if (daysSinceLastStart > Math.max(45, avgCycleLength + 10)) {
    findings.push({
      issue: `No period logged for ${daysSinceLastStart} days`,
      possibleCauses:
        'A missed/late period can be pregnancy, high stress, big weight change, PCOS, thyroid, or over-exercising. If pregnancy is possible, test first.',
      severity: 'concern',
    });
  }

  // Lifestyle
  if (smoking === 'regular') {
    findings.push({
      issue: 'Regular smoking',
      possibleCauses: 'Smoking is linked to worse cramps, more irregular cycles and earlier menopause. Cutting down helps your cycle and overall health.',
      severity: 'watch',
    });
  }
  if (alcohol === 'regular') {
    findings.push({
      issue: 'Frequent alcohol',
      possibleCauses: 'Frequent drinking can disrupt the hormones that regulate your cycle and worsen PMS. Keeping it occasional helps.',
      severity: 'watch',
    });
  }

  const hasConcern = findings.some((f) => f.severity === 'concern');
  const status: CycleHealth['status'] = findings.length === 0 ? 'ok' : hasConcern ? 'concern' : 'watch';
  const headline =
    status === 'ok'
      ? 'Your cycle looks regular and healthy — keep it up! 🌿'
      : status === 'concern'
        ? 'A few things are worth getting checked — see the notes below.'
        : 'Mostly fine, with a couple of things to keep an eye on.';

  return {
    status,
    headline,
    findings,
    advice: buildHealthAdvice(findings, smoking, alcohol),
    seeDoctor: hasConcern,
    disclaimer: HEALTH_DISCLAIMER,
  };
}

function baseAdvice(smoking?: string, alcohol?: string): CycleHealth['advice'] {
  return buildHealthAdvice([], smoking, alcohol);
}

function buildHealthAdvice(findings: CycleFinding[], smoking?: string, alcohol?: string): CycleHealth['advice'] {
  const irregularish = findings.some((f) => /irregular|long cycles|missed/i.test(f.issue));
  const diet = [
    'Eat iron-rich foods (leafy greens, dates, legumes, jaggery, or lean red meat) with vitamin C to prevent anaemia from monthly blood loss.',
    'Add omega-3 (walnuts, flax, fatty fish) and magnesium (nuts, seeds, dark chocolate, banana) to ease cramps and mood.',
    'Eat regular, balanced meals — skipping meals and very low-calorie dieting can disrupt your cycle.',
  ];
  if (irregularish) {
    diet.push('For irregular/PCOS-type cycles: favour low-GI carbs (millets, oats, legumes) and limit sugar and refined carbs to steady insulin.');
  }
  const sleep = [
    'Aim for 7–9 hours on a consistent schedule — poor or irregular sleep disrupts the hormones that run your cycle.',
  ];
  const lifestyle = [
    'Manage stress with yoga, breathing or meditation — chronic stress is a common cause of late or missed periods.',
    'Keep a stable, healthy body weight; both very low and rapidly rising weight can disturb periods.',
  ];
  if (smoking === 'regular' || smoking === 'occasional') lifestyle.push('Cutting down or quitting smoking improves cramps, regularity and long-term fertility.');
  if (alcohol === 'regular') lifestyle.push('Keep alcohol occasional — frequent drinking can throw off cycle-regulating hormones.');
  const exercise = [
    'Regular moderate exercise + some strength training supports hormonal balance.',
    'Avoid excessive over-training and very low body fat — they can lighten, delay or stop periods.',
    'Gentle yoga in the days before your period helps with cramps and mood.',
  ];
  return { diet, sleep, lifestyle, exercise };
}
