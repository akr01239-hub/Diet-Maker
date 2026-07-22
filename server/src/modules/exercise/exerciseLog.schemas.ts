import { z } from 'zod';

/** A single performed exercise / set the user is logging. */
export const exerciseLogSchema = z.object({
  exerciseName: z.string().min(1).max(120),
  focus: z.string().max(60).optional(),
  sets: z.number().int().min(0).max(50).optional(),
  reps: z.number().int().min(0).max(1000).optional(),
  weightKg: z.number().min(0).max(1000).optional(),
  durationMin: z.number().int().min(0).max(1000).optional(),
  notes: z.string().max(500).optional(),
  performedAt: z.string().datetime().optional(),
});

export type ExerciseLogBody = z.infer<typeof exerciseLogSchema>;
