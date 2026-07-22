/**
 * Mind & body content: guided yoga flows and meditation/breathing sessions. Static,
 * deterministic, zero-cost (text + timing only — no audio/video hosting). Educational.
 */

export interface YogaPose {
  name: string;
  hold: string; // e.g. "5 breaths", "60s"
  cue: string;
}

export interface YogaFlow {
  id: string;
  name: string;
  focus: string;
  durationMin: number;
  level: 'beginner' | 'all' | 'intermediate';
  poses: YogaPose[];
}

export interface Meditation {
  id: string;
  name: string;
  goal: string;
  durationMin: number;
  steps: string[];
  /** Optional breathing pattern (seconds) the app can animate as a breathing guide. */
  pattern?: { inhaleSec: number; hold1Sec: number; exhaleSec: number; hold2Sec: number };
}

const YOGA: YogaFlow[] = [
  {
    id: 'morning-energizer',
    name: 'Morning Energizer',
    focus: 'Wake up the body & mind',
    durationMin: 12,
    level: 'all',
    poses: [
      { name: 'Cat–Cow', hold: '6 breaths', cue: 'Arch and round with your breath to warm the spine.' },
      { name: 'Downward Dog', hold: '5 breaths', cue: 'Press hips up and back, pedal the heels.' },
      { name: 'Sun Salutation A', hold: '3 rounds', cue: 'Flow with the breath, one movement per inhale/exhale.' },
      { name: 'Warrior I (each side)', hold: '5 breaths', cue: 'Front knee over ankle, arms reaching up.' },
      { name: 'Standing Forward Fold', hold: '5 breaths', cue: 'Soft knees, let the head hang heavy.' },
      { name: 'Mountain Pose + big breath', hold: '5 breaths', cue: 'Stand tall, inhale arms up, exhale down.' },
    ],
  },
  {
    id: 'wind-down',
    name: 'Wind-Down for Sleep',
    focus: 'Calm the nervous system before bed',
    durationMin: 10,
    level: 'beginner',
    poses: [
      { name: "Child's Pose", hold: '8 breaths', cue: 'Knees wide, forehead down, breathe into the back.' },
      { name: 'Reclining Twist (each side)', hold: '6 breaths', cue: 'Drop knees to one side, gaze the other way.' },
      { name: 'Happy Baby', hold: '6 breaths', cue: 'Hold the feet, gently rock side to side.' },
      { name: 'Legs-Up-the-Wall', hold: '3 min', cue: 'Sit close to a wall, swing legs up, relax fully.' },
      { name: 'Savasana', hold: '2 min', cue: 'Lie still, soften the whole body, slow the breath.' },
    ],
  },
  {
    id: 'desk-reset',
    name: 'Desk Reset',
    focus: 'Undo sitting — neck, shoulders, hips',
    durationMin: 7,
    level: 'all',
    poses: [
      { name: 'Neck rolls', hold: '5 each way', cue: 'Slow half-circles; never crank the neck back.' },
      { name: 'Seated Cat–Cow', hold: '6 breaths', cue: 'Move the spine right there in your chair.' },
      { name: 'Seated Spinal Twist (each side)', hold: '5 breaths', cue: 'Lengthen up on the inhale, twist on the exhale.' },
      { name: 'Standing Forward Fold', hold: '6 breaths', cue: 'Release the low back and hamstrings.' },
      { name: 'Low Lunge hip opener (each side)', hold: '6 breaths', cue: 'Sink the hips to open the front of the leg.' },
    ],
  },
  {
    id: 'period-relief',
    name: 'Period Relief',
    focus: 'Gentle poses to ease cramps & bloating',
    durationMin: 12,
    level: 'beginner',
    poses: [
      { name: "Child's Pose", hold: '8 breaths', cue: 'Knees wide, breathe into the lower back.' },
      { name: 'Cat–Cow', hold: '6 breaths', cue: 'Soft, slow spinal waves.' },
      { name: 'Reclining Bound Angle', hold: '2 min', cue: 'Soles together, knees fall open, fully supported.' },
      { name: 'Supine Twist (each side)', hold: '6 breaths', cue: 'Gently wring out the belly and low back.' },
      { name: 'Legs-Up-the-Wall', hold: '3 min', cue: 'Eases bloating and calms the system.' },
    ],
  },
  {
    id: 'core-strength',
    name: 'Core & Strength Flow',
    focus: 'Build stability and heat',
    durationMin: 15,
    level: 'intermediate',
    poses: [
      { name: 'Sun Salutation B', hold: '3 rounds', cue: 'Chair, lunge, plank, updog, downdog — flowing.' },
      { name: 'Plank', hold: '45s', cue: 'Straight line head to heels, brace the belly.' },
      { name: 'Side Plank (each side)', hold: '30s', cue: 'Stack the shoulders, lift the hips.' },
      { name: 'Boat Pose', hold: '5 breaths ×3', cue: 'Lift the chest, long spine, don’t collapse.' },
      { name: 'Bridge', hold: '6 breaths', cue: 'Press through the feet, lift the hips.' },
    ],
  },
];

const MEDITATION: Meditation[] = [
  {
    id: 'box-breathing',
    name: 'Box Breathing',
    goal: 'Focus & calm in 4 minutes',
    durationMin: 4,
    steps: [
      'Sit tall, shoulders relaxed, eyes soft or closed.',
      'Inhale through the nose for 4 counts.',
      'Hold gently for 4 counts.',
      'Exhale slowly for 4 counts.',
      'Hold empty for 4 counts, then repeat the box.',
    ],
    pattern: { inhaleSec: 4, hold1Sec: 4, exhaleSec: 4, hold2Sec: 4 },
  },
  {
    id: '478-sleep',
    name: '4-7-8 for Sleep',
    goal: 'Wind down & fall asleep',
    durationMin: 5,
    steps: [
      'Lie down or sit comfortably; rest the tongue behind the top teeth.',
      'Inhale quietly through the nose for 4 counts.',
      'Hold the breath for 7 counts.',
      'Exhale fully through the mouth for 8 counts.',
      'Repeat for 4–8 rounds; let each exhale relax you deeper.',
    ],
    pattern: { inhaleSec: 4, hold1Sec: 7, exhaleSec: 8, hold2Sec: 0 },
  },
  {
    id: 'stress-reset',
    name: 'Stress Reset',
    goal: 'Quickly settle a racing mind',
    durationMin: 3,
    steps: [
      'Take one big sigh out to signal "safe" to your body.',
      'Breathe in for 4, out slowly for 6 — a longer exhale calms the nerves.',
      'Name 5 things you can see, 4 you can hear, 3 you can feel.',
      'Return to the slow breath for a few more rounds.',
    ],
    pattern: { inhaleSec: 4, hold1Sec: 0, exhaleSec: 6, hold2Sec: 0 },
  },
  {
    id: 'body-scan',
    name: 'Body Scan',
    goal: 'Release tension head to toe',
    durationMin: 8,
    steps: [
      'Lie down comfortably and let the breath be natural.',
      'Bring attention to the top of your head; notice any sensation without judging.',
      'Slowly move down — face, neck, shoulders, arms, chest, belly, hips, legs, feet.',
      'Soften each area on the exhale as you pass through it.',
      'Rest for a few breaths feeling the whole body at once.',
    ],
  },
  {
    id: 'gratitude',
    name: 'Morning Gratitude',
    goal: 'Start the day grounded',
    durationMin: 4,
    steps: [
      'Sit comfortably and take three slow breaths.',
      'Bring to mind one person you are grateful for — picture them clearly.',
      'Bring to mind one thing about your body or health you are grateful for.',
      'Set one simple intention for the day.',
      'Breathe it in, then gently begin your morning.',
    ],
  },
];

export function getWellness(): { yoga: YogaFlow[]; meditation: Meditation[] } {
  return { yoga: YOGA, meditation: MEDITATION };
}
