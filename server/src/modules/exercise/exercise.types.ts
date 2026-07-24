export type ExerciseLocation = 'gym' | 'home' | 'none';
export type BodyGoal = 'fatloss' | 'athletic' | 'muscular';

/** Progressive-overload hint for the next session, derived from the user's logged history. */
export interface NextSession {
  suggestedWeightKg: number | null;
  suggestedReps: number;
  suggestedSets: number;
  deload: boolean;
  rationale: string;
}

export interface ExerciseItem {
  name: string;
  sets: number;
  reps: string; // "8-10", "20 min", "45s"
  type: 'strength' | 'cardio' | 'mobility';
  /** Present once the user has logged this exercise before. */
  nextSession?: NextSession;
}

export interface WorkoutDay {
  dayIndex: number; // 0..6
  date?: string; // YYYY-MM-DD
  label?: string; // Today / Tomorrow / weekday
  focus: string; // "Push (Chest/Shoulders/Triceps)", "Rest & recovery"
  rest: boolean;
  exercises: ExerciseItem[];
}

export interface WeeklyWorkout {
  location: ExerciseLocation;
  goal: BodyGoal;
  days: WorkoutDay[];
  /** 0-based mesocycle block; rotates every 4 weeks so training keeps progressing. */
  block: number;
  blockLabel: string; // "Month 1 · Block A"
  note: string;
  disclaimer: string;
}
