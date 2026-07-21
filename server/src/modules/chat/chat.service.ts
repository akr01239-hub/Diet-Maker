import { prisma } from '../../lib/prisma';
import { decryptJson } from '../../lib/crypto';
import type { FoodItem, MealSlot } from '../food/food.types';
import { answer, type ChatReply } from './chat.engine';

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

/** Fuzzy-ish food matcher: name contains the term, or the term contains a name keyword. */
function makeFindFood(foods: FoodItem[]) {
  const stop = new Set(['and', 'the', 'with', 'dal', 'curry', 'cooked', 'plain']);
  return (term: string): FoodItem | undefined => {
    const t = term.toLowerCase().trim();
    if (!t) return undefined;
    let match = foods.find((f) => f.name.toLowerCase().includes(t));
    if (match) return match;
    match = foods.find((f) =>
      f.name
        .toLowerCase()
        .split(/[^a-z]+/)
        .some((w) => w.length > 2 && !stop.has(w) && t.includes(w)),
    );
    return match;
  };
}

export async function chat(userId: string, message: string, firstName?: string): Promise<ChatReply> {
  const [snapshot, profile, foods] = await Promise.all([
    prisma.calcResultSnapshot.findFirst({ where: { userId }, orderBy: { createdAt: 'desc' } }),
    prisma.profile.findUnique({ where: { userId } }),
    prisma.food.findMany(),
  ]);

  const result = snapshot?.result as
    | { dailyKcal: number; proteinG: number; waterMl: number }
    | undefined;

  let conditions: string[] = [];
  if (profile?.sensitiveEnc) {
    try {
      conditions = decryptJson<{ conditions?: string[] }>(profile.sensitiveEnc).conditions ?? [];
    } catch {
      conditions = [];
    }
  }

  const reply = answer(message, {
    targets: result ? { dailyKcal: result.dailyKcal, proteinG: result.proteinG, waterMl: result.waterMl } : null,
    conditions,
    findFood: makeFindFood(foods.map(toFoodItem)),
    firstName,
  });

  await prisma.auditLog.create({ data: { userId, action: 'chat.query', detail: reply.intent } });
  return reply;
}
