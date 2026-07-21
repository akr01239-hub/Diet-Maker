import { prisma } from '../../lib/prisma';
import { decryptJson, encryptJson } from '../../lib/crypto';
import { HttpError } from '../../middleware/error';
import { computeCalcResult } from '../nutrition/calcResult';
import { ageFromDob } from '../nutrition/calc.service';
import { generateWeekPlan } from '../food/planGenerator';
import type { SensitiveData } from '../profile/profile.schemas';
import type { ActivityLevel, Goal } from '../../calc/types';
import type { Condition } from '../../guardrails';
import type { FoodItem, MealSlot, PlanTargets } from '../food/food.types';

export interface FamilyMemberInput {
  firstName: string;
  relation?: string;
  heightCm: number;
  activityLevel: string;
  goal: string;
  dietType: string;
  reducedMobility?: boolean;
  sensitive: SensitiveData;
}

export async function addMember(ownerId: string, body: FamilyMemberInput) {
  const member = await prisma.familyMember.create({
    data: {
      ownerId,
      firstName: body.firstName,
      relation: body.relation ?? null,
      heightCm: body.heightCm,
      activityLevel: body.activityLevel,
      goal: body.goal,
      dietType: body.dietType,
      reducedMobility: body.reducedMobility ?? false,
      sensitiveEnc: encryptJson(body.sensitive),
    },
  });
  await prisma.auditLog.create({ data: { userId: ownerId, action: 'family.add', detail: member.id } });
  return { id: member.id, firstName: member.firstName, relation: member.relation };
}

export async function listMembers(ownerId: string) {
  const rows = await prisma.familyMember.findMany({ where: { ownerId }, orderBy: { createdAt: 'asc' } });
  return rows.map((m) => ({
    id: m.id,
    firstName: m.firstName,
    relation: m.relation,
    heightCm: m.heightCm,
    activityLevel: m.activityLevel,
    goal: m.goal,
    dietType: m.dietType,
  }));
}

/** Loads a member scoped to the owner, or 404. */
async function ownedMember(ownerId: string, memberId: string) {
  const m = await prisma.familyMember.findFirst({ where: { id: memberId, ownerId } });
  if (!m) throw new HttpError(404, 'Family member not found');
  return m;
}

export async function memberCalc(ownerId: string, memberId: string) {
  const m = await ownedMember(ownerId, memberId);
  const s = decryptJson<SensitiveData>(m.sensitiveEnc);
  return computeCalcResult({
    heightCm: m.heightCm,
    currentWeightKg: s.currentWeightKg,
    targetWeightKg: s.targetWeightKg,
    ageYears: ageFromDob(s.dob),
    sex: s.sex,
    activityLevel: m.activityLevel as ActivityLevel,
    goal: m.goal as Goal,
    waistCm: s.waistCm,
    conditions: s.conditions as Condition[],
    desiredWeeklyLossKg: s.desiredWeeklyLossKg,
    reducedMobility: m.reducedMobility,
  });
}

function toFoodItem(f: {
  id: string; name: string; locale: string; region: string | null; category: string;
  mealSlots: string[]; kcal: number; proteinG: number; carbG: number; fatG: number;
  fiberG: number; sugarG: number; sodiumMg: number; glycemicIndex: number | null;
  typicalServingG: number; costTier: number; tags: string[]; allergens: string[];
}): FoodItem {
  return {
    id: f.id, name: f.name, locale: f.locale, region: f.region ?? undefined,
    category: f.category as FoodItem['category'], mealSlots: f.mealSlots as MealSlot[],
    kcal: f.kcal, proteinG: f.proteinG, carbG: f.carbG, fatG: f.fatG, fiberG: f.fiberG,
    sugarG: f.sugarG, sodiumMg: f.sodiumMg, glycemicIndex: f.glycemicIndex ?? undefined,
    typicalServingG: f.typicalServingG, costTier: (f.costTier as 1 | 2 | 3) ?? 2,
    tags: f.tags, allergens: f.allergens,
  };
}

export async function memberPlan(ownerId: string, memberId: string, days = 7) {
  const m = await ownedMember(ownerId, memberId);
  const s = decryptJson<SensitiveData>(m.sensitiveEnc);
  const calc = await memberCalc(ownerId, memberId);
  const targets: PlanTargets = {
    dailyKcal: calc.dailyKcal,
    proteinG: calc.proteinG,
    fatG: calc.fatG,
    carbG: calc.carbG,
    fiberG: calc.fiberG,
    sodiumMaxMg: calc.sodiumMaxMg,
  };
  const foods = await prisma.food.findMany();
  return generateWeekPlan(
    foods.map(toFoodItem),
    targets,
    { dietType: m.dietType, allergies: s.allergies, conditions: s.conditions, locale: 'IN' },
    { days },
  );
}

export async function removeMember(ownerId: string, memberId: string) {
  const res = await prisma.familyMember.deleteMany({ where: { id: memberId, ownerId } });
  if (res.count === 0) throw new HttpError(404, 'Family member not found');
}
