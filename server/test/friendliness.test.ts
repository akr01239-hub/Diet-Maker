import { describe, it, expect } from 'vitest';
import { foodFriendliness, mealFriendliness } from '../src/modules/food/friendliness';
import type { FoodItem, MealItem } from '../src/modules/food/food.types';

function food(over: Partial<FoodItem>): FoodItem {
  return {
    id: 'x', name: 'x', locale: 'IN', category: 'vegetarian', mealSlots: ['lunch'],
    kcal: 100, proteinG: 3, carbG: 15, fatG: 2, fiberG: 3, sugarG: 2, sodiumMg: 50,
    typicalServingG: 100, costTier: 1, tags: [], allergens: [], ...over,
  };
}

describe('foodFriendliness', () => {
  it('rates a fibre-rich low-GI food high for diabetes; a fried sugary food low', () => {
    const dal = foodFriendliness(food({ glycemicIndex: 30, fiberG: 8, sugarG: 1, tags: ['legume', 'high-fiber'] }));
    const jalebi = foodFriendliness(food({ glycemicIndex: 85, sugarG: 40, tags: ['refined', 'fried', 'high-sugar', 'high-gi'] }));
    expect(dal.diabetes).toBeGreaterThan(jalebi.diabetes);
    expect(dal.inflammation).toBeGreaterThan(jalebi.inflammation);
  });

  it('rewards fermented/probiotic foods for gut health', () => {
    const curd = foodFriendliness(food({ tags: ['dairy', 'probiotic'], fiberG: 0 }));
    const maida = foodFriendliness(food({ tags: ['refined'], fiberG: 0 }));
    expect(curd.gut).toBeGreaterThan(maida.gut);
  });

  it('penalises high sodium + saturated fat for heart', () => {
    const clean = foodFriendliness(food({ sodiumMg: 20, fiberG: 5 }));
    const salty = foodFriendliness(food({ sodiumMg: 900, tags: ['high-sodium', 'high-satfat', 'fried'] }));
    expect(clean.heart).toBeGreaterThan(salty.heart);
  });

  it('all scores stay within 0..100', () => {
    for (const f of [food({ sugarG: 99, sodiumMg: 2000, tags: ['fried', 'refined', 'high-sugar', 'high-satfat', 'high-sodium', 'high-gi'] }), food({ fiberG: 30, tags: ['probiotic', 'high-fiber'] })]) {
      const s = foodFriendliness(f);
      for (const v of Object.values(s)) expect(v).toBeGreaterThanOrEqual(0), expect(v).toBeLessThanOrEqual(100);
    }
  });
});

describe('mealFriendliness', () => {
  it('aggregates by grams and surfaces highlight labels', () => {
    const dal = food({ id: 'dal', glycemicIndex: 30, fiberG: 8, sugarG: 1, tags: ['legume', 'high-fiber'] });
    const roti = food({ id: 'roti', glycemicIndex: 45, fiberG: 4, sodiumMg: 30 });
    const byId = new Map([[dal.id, dal], [roti.id, roti]]);
    const items: MealItem[] = [
      { foodId: 'dal', name: 'dal', grams: 150, kcal: 150, proteinG: 9, carbG: 20, fatG: 3, fiberG: 12, sugarG: 1, sodiumMg: 200 },
      { foodId: 'roti', name: 'roti', grams: 80, kcal: 200, proteinG: 6, carbG: 40, fatG: 2, fiberG: 3, sugarG: 1, sodiumMg: 25 },
    ];
    const m = mealFriendliness(items, byId);
    expect(m.diabetes).toBeGreaterThan(0);
    expect(Array.isArray(m.highlights)).toBe(true);
    expect(m.highlights.length).toBeLessThanOrEqual(2);
  });

  it('ignores items whose food is missing', () => {
    const m = mealFriendliness(
      [{ foodId: 'ghost', name: 'x', grams: 100, kcal: 100, proteinG: 1, carbG: 1, fatG: 1, fiberG: 1, sugarG: 1, sodiumMg: 1 }],
      new Map(),
    );
    expect(m.diabetes).toBe(0);
  });
});
