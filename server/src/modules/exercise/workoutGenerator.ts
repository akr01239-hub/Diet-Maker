import {
  BodyGoal,
  ExerciseItem,
  ExerciseLocation,
  WeeklyWorkout,
  WorkoutDay,
} from './exercise.types';

const s = (name: string, sets: number, reps: string): ExerciseItem => ({ name, sets, reps, type: 'strength' });
const c = (name: string, sets: number, reps: string): ExerciseItem => ({ name, sets, reps, type: 'cardio' });
const m = (name: string, sets: number, reps: string): ExerciseItem => ({ name, sets, reps, type: 'mobility' });

interface DayTemplate {
  focus: string;
  exercises: ExerciseItem[];
}

const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

/** 6 training-day templates per (goal, location). Rest is inserted at plan time. */
const TEMPLATES: Record<string, DayTemplate[]> = {
  'muscular:gym': [
    { focus: 'Push (Chest / Shoulders / Triceps)', exercises: [s('Barbell bench press', 4, '8-10'), s('Incline dumbbell press', 3, '10-12'), s('Seated shoulder press', 3, '8-10'), s('Lateral raise', 3, '12-15'), s('Tricep rope pushdown', 3, '12')] },
    { focus: 'Pull (Back / Biceps)', exercises: [s('Deadlift', 4, '6-8'), s('Lat pulldown', 4, '10-12'), s('Seated cable row', 3, '10-12'), s('Barbell curl', 3, '10'), s('Face pull', 3, '15')] },
    { focus: 'Legs', exercises: [s('Back squat', 4, '8-10'), s('Leg press', 3, '12'), s('Romanian deadlift', 3, '10'), s('Leg curl', 3, '12'), s('Standing calf raise', 4, '15')] },
    { focus: 'Push (Hypertrophy)', exercises: [s('Incline barbell press', 4, '8-10'), s('Dumbbell shoulder press', 3, '10'), s('Cable chest fly', 3, '12'), s('Dips', 3, '10'), s('Overhead tricep extension', 3, '12')] },
    { focus: 'Pull (Width & Arms)', exercises: [s('Pull-ups', 4, '8'), s('Barbell row', 4, '8-10'), s('Chest-supported row', 3, '12'), s('Hammer curl', 3, '12'), s('Rear delt fly', 3, '15')] },
    { focus: 'Legs & Core', exercises: [s('Front squat', 4, '8'), s('Walking lunges', 3, '12'), s('Leg extension', 3, '15'), s('Seated calf raise', 4, '20'), s('Hanging leg raise', 3, '15')] },
  ],
  'muscular:home': [
    { focus: 'Push (Chest / Shoulders / Triceps)', exercises: [s('Push-ups', 4, '15-20'), s('Pike push-ups', 3, '10'), s('Diamond push-ups', 3, '12'), s('Chair dips', 3, '15'), m('Plank', 3, '45s')] },
    { focus: 'Pull (Back / Biceps)', exercises: [s('Backpack bent-over rows', 4, '12'), s('Doorway rows', 3, '12'), s('Superman', 3, '15'), s('Reverse snow-angels', 3, '15'), s('Backpack curls', 3, '15')] },
    { focus: 'Legs', exercises: [s('Bodyweight squats', 4, '20'), s('Reverse lunges', 3, '15'), s('Glute bridges', 3, '20'), s('Wall sit', 3, '45s'), s('Calf raises', 4, '25')] },
    { focus: 'Push (Advanced)', exercises: [s('Feet-elevated push-ups', 4, '12'), s('Archer push-ups', 3, '8'), s('Pseudo-planche push-ups', 3, '8'), s('Chair dips', 3, '15'), s('Pike push-ups', 3, '10')] },
    { focus: 'Pull & Shoulders', exercises: [s('Backpack rows', 4, '15'), s('Towel curls', 3, '15'), s('Superman pulls', 3, '15'), s('Face-down Y-raise', 3, '15'), s('Backpack shrugs', 3, '20')] },
    { focus: 'Legs & Core', exercises: [s('Bulgarian split squat (chair)', 3, '12'), s('Jump squats', 3, '15'), s('Single-leg glute bridge', 3, '12'), s('Step-ups', 3, '15'), s('Leg raises', 3, '15')] },
  ],
  'athletic:gym': [
    { focus: 'Upper strength', exercises: [s('Bench press', 4, '6'), s('Weighted pull-ups', 4, '6'), s('Overhead press', 3, '8'), s('Barbell row', 3, '8'), m('Plank', 3, '45s')] },
    { focus: 'Lower strength & core', exercises: [s('Back squat', 4, '6'), s('Romanian deadlift', 3, '8'), s('Walking lunges', 3, '12'), s('Hanging leg raise', 3, '15'), s('Calf raise', 3, '20')] },
    { focus: 'HIIT conditioning', exercises: [c('Rowing intervals', 10, '30s hard / 30s easy'), s('Kettlebell swings', 4, '20'), c('Battle ropes', 4, '30s'), c('Burpees', 4, '12')] },
    { focus: 'Upper hypertrophy', exercises: [s('Incline dumbbell press', 4, '10'), s('Lat pulldown', 4, '12'), s('Lateral raise', 4, '15'), s('Cable curl', 3, '12'), s('Tricep pushdown', 3, '12')] },
    { focus: 'Lower power', exercises: [c('Box jumps', 5, '5'), s('Trap-bar deadlift', 4, '5'), s('Bulgarian split squat', 3, '10'), s('Nordic curl', 3, '8'), m('Plank', 3, '60s')] },
    { focus: 'Cardio & mobility', exercises: [c('Steady run', 1, '30-40 min'), m('Mobility flow', 1, '15 min'), s('Core circuit', 3, 'rounds')] },
  ],
  'athletic:home': [
    { focus: 'Full-body circuit', exercises: [s('Push-ups', 3, '15'), s('Bodyweight squats', 3, '20'), m('Plank', 3, '45s'), s('Reverse lunges', 3, '12'), s('Superman', 3, '15')] },
    { focus: 'HIIT', exercises: [c('Burpees', 5, '12'), c('High knees', 5, '30s'), c('Jump squats', 5, '15'), c('Mountain climbers', 5, '30s')] },
    { focus: 'Core & mobility', exercises: [m('Plank', 3, '60s'), s('Leg raises', 3, '15'), s('Russian twists', 3, '20'), s('Dead bug', 3, '12'), m('Mobility flow', 1, '10 min')] },
    { focus: 'Full-body circuit', exercises: [s('Pike push-ups', 3, '10'), s('Reverse lunges', 3, '15'), s('Chair dips', 3, '15'), s('Glute bridges', 3, '20'), s('Superman', 3, '15')] },
    { focus: 'Plyometrics', exercises: [c('Jump squats', 4, '15'), c('Broad jumps', 4, '8'), c('Skater jumps', 4, '20'), c('Burpees', 4, '10'), c('Plank jacks', 4, '20')] },
    { focus: 'Cardio & core', exercises: [c('Brisk walk / jog', 1, '30-40 min'), s('Core circuit', 3, 'rounds')] },
  ],
  'fatloss:gym': [
    { focus: 'Full-body weights', exercises: [s('Back squat', 3, '10'), s('Bench press', 3, '10'), s('Seated row', 3, '10'), s('Overhead press', 3, '10'), m('Plank', 3, '45s')] },
    { focus: 'HIIT', exercises: [c('Treadmill sprints', 10, '30s on / 60s off'), s('Kettlebell swings', 4, '20'), c('Mountain climbers', 4, '30s')] },
    { focus: 'Full-body weights', exercises: [s('Deadlift', 3, '8'), s('Incline press', 3, '10'), s('Lat pulldown', 3, '12'), s('Walking lunges', 3, '12'), s('Hanging knee raise', 3, '15')] },
    { focus: 'Steady cardio & core', exercises: [c('Incline walk / cycle', 1, '40-45 min'), s('Core circuit', 3, 'rounds')] },
    { focus: 'Full-body weights', exercises: [s('Leg press', 3, '12'), s('Dumbbell press', 3, '12'), s('Cable row', 3, '12'), s('Lateral raise', 3, '15'), s('Russian twist', 3, '20')] },
    { focus: 'HIIT & core', exercises: [c('Bike intervals', 12, '20s on / 40s off'), c('Burpees', 4, '12'), m('Plank complex', 3, 'rounds')] },
  ],
  'fatloss:home': [
    { focus: 'Cardio & circuit', exercises: [c('Brisk walk / jog', 1, '25-30 min'), s('Bodyweight squats', 3, '20'), s('Push-ups', 3, '12'), m('Plank', 3, '40s')] },
    { focus: 'HIIT', exercises: [c('Jumping jacks', 5, '40s'), c('Burpees', 5, '10'), c('Mountain climbers', 5, '30s'), c('High knees', 5, '30s')] },
    { focus: 'Core & walk', exercises: [m('Plank', 3, '45s'), s('Leg raises', 3, '15'), s('Bicycle crunches', 3, '20'), c('Walk', 1, '20-30 min')] },
    { focus: 'Bodyweight circuit', exercises: [s('Reverse lunges', 3, '15'), s('Push-ups', 3, '12'), s('Glute bridges', 3, '20'), s('Superman', 3, '15'), s('Wall sit', 3, '45s')] },
    { focus: 'HIIT', exercises: [c('Squat jumps', 5, '15'), c('Skater jumps', 5, '20'), c('Burpees', 5, '10'), c('Plank jacks', 5, '20')] },
    { focus: 'Steady cardio', exercises: [c('Brisk walk / cycle', 1, '35-40 min'), m('Stretching', 1, '10 min')] },
  ],
};

const REST_DAY: ExerciseItem[] = [
  c('Light walk', 1, '20-30 min'),
  m('Full-body stretching', 1, '10 min'),
];

const DISCLAIMER =
  'Educational guidance, not a substitute for a doctor or certified trainer. Warm up, use good form, and stop if you feel pain.';

export interface WorkoutOptions {
  restDayOfWeek?: number; // 0=Sun..6=Sat; omitted => 7 training days
  startDate?: Date;
  days?: number;
}

/**
 * Deterministic weekly workout generator. Given goal + location (+ optional rest day),
 * assembles a day-by-day plan. No LLM — same inputs, same plan.
 */
export function generateWeeklyWorkout(
  goal: BodyGoal,
  location: ExerciseLocation,
  options: WorkoutOptions = {},
): WeeklyWorkout {
  // 'none' (no equipment at all) reuses the home templates.
  const key = `${goal}:${location === 'none' ? 'home' : location}`;
  const templates = TEMPLATES[key] ?? TEMPLATES[`${goal}:home`]!;
  const dayCount = options.days ?? 7;

  const days: WorkoutDay[] = [];
  let t = 0;
  for (let d = 0; d < dayCount; d++) {
    let date: string | undefined;
    let weekday = d;
    let baseLabel: string | undefined;
    if (options.startDate) {
      const dt = new Date(options.startDate.getTime() + d * 86_400_000);
      weekday = dt.getUTCDay();
      date = dt.toISOString().slice(0, 10);
      baseLabel = d === 0 ? 'Today' : d === 1 ? 'Tomorrow' : WEEKDAYS[weekday];
    }

    const isRest = options.restDayOfWeek !== undefined && weekday === options.restDayOfWeek;
    if (isRest) {
      days.push({ dayIndex: d, date, label: baseLabel ? `${baseLabel} · Rest` : 'Rest', focus: 'Rest & recovery', rest: true, exercises: REST_DAY });
    } else {
      const tmpl = templates[t % templates.length]!;
      t++;
      days.push({ dayIndex: d, date, label: baseLabel, focus: tmpl.focus, rest: false, exercises: tmpl.exercises });
    }
  }

  const note =
    location === 'none'
      ? 'No equipment needed — bodyweight only. A full 7-day plan with lighter days built in.'
      : options.restDayOfWeek === undefined
        ? '7-day plan (no fixed rest day). Add a rest day in your profile if you prefer 6 days.'
        : '6 training days + 1 rest day. Progress by adding reps or load each week.';

  return { location, goal, days, note, disclaimer: DISCLAIMER };
}
