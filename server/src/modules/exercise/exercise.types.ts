export type ExerciseLocation = 'gym' | 'home' | 'none';
export type BodyGoal = 'fatloss' | 'athletic' | 'muscular';

export interface ExerciseItem {
  name: string;
  sets: number;
  reps: string; // "8-10", "20 min", "45s"
  type: 'strength' | 'cardio' | 'mobility';
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
