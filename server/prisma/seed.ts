import { PrismaClient } from '@prisma/client';
import { SEED_FOODS } from '../src/data/foods.seed';

const prisma = new PrismaClient();

async function main() {
  console.log(`Seeding ${SEED_FOODS.length} foods...`);
  for (const f of SEED_FOODS) {
    await prisma.food.upsert({
      where: { id: f.id },
      update: {
        name: f.name,
        locale: f.locale,
        region: f.region ?? null,
        category: f.category,
        mealSlots: f.mealSlots,
        kcal: f.kcal,
        proteinG: f.proteinG,
        carbG: f.carbG,
        fatG: f.fatG,
        fiberG: f.fiberG,
        sugarG: f.sugarG,
        sodiumMg: f.sodiumMg,
        glycemicIndex: f.glycemicIndex ?? null,
        typicalServingG: f.typicalServingG,
        costTier: f.costTier,
        tags: f.tags,
        allergens: f.allergens,
        source: 'seed',
      },
      create: {
        id: f.id,
        name: f.name,
        locale: f.locale,
        region: f.region ?? null,
        category: f.category,
        mealSlots: f.mealSlots,
        kcal: f.kcal,
        proteinG: f.proteinG,
        carbG: f.carbG,
        fatG: f.fatG,
        fiberG: f.fiberG,
        sugarG: f.sugarG,
        sodiumMg: f.sodiumMg,
        glycemicIndex: f.glycemicIndex ?? null,
        typicalServingG: f.typicalServingG,
        costTier: f.costTier,
        tags: f.tags,
        allergens: f.allergens,
        source: 'seed',
      },
    });
  }
  const count = await prisma.food.count();
  console.log(`Done. Foods in DB: ${count}`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
