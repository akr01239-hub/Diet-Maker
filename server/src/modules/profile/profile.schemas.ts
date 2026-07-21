import { z } from 'zod';

export const CONDITIONS = [
  'diabetes',
  'hypertension',
  'kidney_disease',
  'fatty_liver',
  'thyroid',
  'pcos',
  'pregnancy',
  'breastfeeding',
  'gout',
  'heart_disease',
  'cancer',
  'post_surgery',
] as const;

export const ACTIVITY_LEVELS = [
  'sedentary',
  'light',
  'moderate',
  'active',
  'veryactive',
] as const;

export const GOALS = ['lose', 'maintain', 'gain'] as const;

export const DIET_TYPES = [
  'veg',
  'eggetarian',
  'nonveg',
  'vegan',
  'jain',
  'keto',
  'mediterranean',
  'lowcarb',
  'highprotein',
  'satvik',
  'if',
  'custom',
] as const;

/** Sensitive health payload — stored AES-256-GCM-encrypted at rest. */
export const sensitiveSchema = z.object({
  sex: z.enum(['male', 'female']),
  dob: z.string().date(),
  currentWeightKg: z.number().positive().max(500),
  targetWeightKg: z.number().positive().max(500),
  targetDate: z.string().date().optional(),
  waistCm: z.number().positive().max(300).optional(),
  chestCm: z.number().positive().max(300).optional(),
  hipCm: z.number().positive().max(300).optional(),
  neckCm: z.number().positive().max(300).optional(),
  bloodPressure: z.string().max(20).optional(),
  restingHr: z.number().int().positive().max(250).optional(),
  bloodSugar: z.number().positive().max(1000).optional(),
  conditions: z.array(z.enum(CONDITIONS)).default([]),
  allergies: z.array(z.string().max(60)).default([]),
  desiredWeeklyLossKg: z.number().positive().max(3).optional(),
  clinicianOverride: z.boolean().optional(),
});

export const profileUpsertSchema = z.object({
  heightCm: z.number().positive().max(272),
  activityLevel: z.enum(ACTIVITY_LEVELS).default('moderate'),
  goal: z.enum(GOALS).default('maintain'),
  dietType: z.enum(DIET_TYPES).default('nonveg'),
  reducedMobility: z.boolean().default(false),
  sensitive: sensitiveSchema,
});

export type SensitiveData = z.infer<typeof sensitiveSchema>;
export type ProfileUpsertBody = z.infer<typeof profileUpsertSchema>;

/** Body for the unauthenticated onboarding calc preview. */
export const calcPreviewSchema = z.object({
  heightCm: z.number().positive().max(272),
  currentWeightKg: z.number().positive().max(500),
  targetWeightKg: z.number().positive().max(500),
  ageYears: z.number().int().positive().max(120),
  sex: z.enum(['male', 'female']),
  activityLevel: z.enum(ACTIVITY_LEVELS),
  goal: z.enum(GOALS),
  waistCm: z.number().positive().max(300).optional(),
  conditions: z.array(z.enum(CONDITIONS)).default([]),
  desiredWeeklyLossKg: z.number().positive().max(3).optional(),
  clinicianOverride: z.boolean().optional(),
  reducedMobility: z.boolean().optional(),
});
