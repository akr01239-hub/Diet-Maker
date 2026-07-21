import { FoodItem } from '../modules/food/food.types';

/**
 * Seed food set — common Indian + global foods with approximate per-100g values
 * (USDA / IFCT-style). Not exhaustive; the ETL can expand this from USDA FDC later.
 * Tags drive condition guardrails (e.g. 'high-sugar', 'high-sodium', 'onion').
 */
export const SEED_FOODS: FoodItem[] = [
  // ---- wake-up / hydration / light ----
  { id: 'soaked-almonds', name: 'Soaked almonds', locale: 'IN', category: 'vegan', mealSlots: ['wakeup', 'midmorning', 'eveningsnack'], kcal: 579, proteinG: 21, carbG: 22, fatG: 50, fiberG: 12, sugarG: 4, sodiumMg: 1, glycemicIndex: 15, typicalServingG: 15, costTier: 3, tags: ['nuts', 'healthy-fat'], allergens: ['tree_nut'] },
  { id: 'green-tea', name: 'Green tea (unsweetened)', locale: 'global', category: 'vegan', mealSlots: ['wakeup', 'eveningsnack'], kcal: 1, proteinG: 0, carbG: 0, fatG: 0, fiberG: 0, sugarG: 0, sodiumMg: 1, typicalServingG: 200, costTier: 1, tags: ['beverage'], allergens: [] },

  // ---- breakfast ----
  { id: 'poha', name: 'Poha (flattened rice, cooked)', locale: 'IN', region: 'West', category: 'vegan', mealSlots: ['breakfast', 'midmorning'], kcal: 130, proteinG: 2.6, carbG: 27, fatG: 1.5, fiberG: 1.2, sugarG: 1, sodiumMg: 180, glycemicIndex: 60, typicalServingG: 200, costTier: 1, tags: ['grain'], allergens: [] },
  { id: 'idli', name: 'Idli (steamed)', locale: 'IN', region: 'South', category: 'vegan', mealSlots: ['breakfast', 'dinner'], kcal: 146, proteinG: 4, carbG: 30, fatG: 0.5, fiberG: 1.3, sugarG: 0.5, sodiumMg: 200, glycemicIndex: 55, typicalServingG: 150, costTier: 1, tags: ['grain', 'fermented'], allergens: [] },
  { id: 'oats-porridge', name: 'Oats porridge (with water)', locale: 'global', category: 'vegan', mealSlots: ['breakfast'], kcal: 71, proteinG: 2.5, carbG: 12, fatG: 1.5, fiberG: 1.7, sugarG: 0.5, sodiumMg: 4, glycemicIndex: 55, typicalServingG: 250, costTier: 1, tags: ['grain', 'high-fiber'], allergens: ['gluten'] },
  { id: 'boiled-egg', name: 'Boiled egg', locale: 'global', category: 'egg', mealSlots: ['breakfast', 'eveningsnack'], kcal: 155, proteinG: 13, carbG: 1.1, fatG: 11, fiberG: 0, sugarG: 1.1, sodiumMg: 124, typicalServingG: 100, costTier: 1, tags: ['high-protein'], allergens: ['egg'] },
  { id: 'moong-chilla', name: 'Moong dal chilla', locale: 'IN', category: 'vegan', mealSlots: ['breakfast', 'dinner'], kcal: 170, proteinG: 11, carbG: 20, fatG: 5, fiberG: 4, sugarG: 1, sodiumMg: 190, glycemicIndex: 40, typicalServingG: 150, costTier: 1, tags: ['legume', 'high-protein', 'high-fiber', 'onion'], allergens: [] },
  { id: 'aloo-paratha', name: 'Aloo paratha', locale: 'IN', region: 'North', category: 'vegetarian', mealSlots: ['breakfast'], kcal: 250, proteinG: 5, carbG: 36, fatG: 9, fiberG: 3, sugarG: 1.5, sodiumMg: 320, glycemicIndex: 70, typicalServingG: 120, costTier: 1, tags: ['grain', 'refined', 'high-gi'], allergens: ['gluten'] },

  // ---- mid-morning / snacks / fruit ----
  { id: 'banana', name: 'Banana', locale: 'global', category: 'vegan', mealSlots: ['midmorning', 'eveningsnack', 'wakeup'], kcal: 89, proteinG: 1.1, carbG: 23, fatG: 0.3, fiberG: 2.6, sugarG: 12, sodiumMg: 1, glycemicIndex: 51, typicalServingG: 120, costTier: 1, tags: ['fruit'], allergens: [] },
  { id: 'apple', name: 'Apple', locale: 'global', category: 'vegan', mealSlots: ['midmorning', 'eveningsnack'], kcal: 52, proteinG: 0.3, carbG: 14, fatG: 0.2, fiberG: 2.4, sugarG: 10, sodiumMg: 1, glycemicIndex: 36, typicalServingG: 150, costTier: 2, tags: ['fruit', 'high-fiber'], allergens: [] },
  { id: 'buttermilk', name: 'Buttermilk (chaas)', locale: 'IN', category: 'vegetarian', mealSlots: ['midmorning', 'lunch'], kcal: 40, proteinG: 3.3, carbG: 4.8, fatG: 0.9, fiberG: 0, sugarG: 4.8, sodiumMg: 105, typicalServingG: 200, costTier: 1, tags: ['dairy', 'probiotic'], allergens: ['milk'] },
  { id: 'roasted-chana', name: 'Roasted chana', locale: 'IN', category: 'vegan', mealSlots: ['eveningsnack', 'midmorning'], kcal: 364, proteinG: 20, carbG: 61, fatG: 6, fiberG: 18, sugarG: 5, sodiumMg: 20, glycemicIndex: 35, typicalServingG: 40, costTier: 1, tags: ['legume', 'high-protein', 'high-fiber'], allergens: [] },
  { id: 'sprouts-salad', name: 'Moong sprouts salad', locale: 'IN', category: 'vegan', mealSlots: ['eveningsnack', 'breakfast'], kcal: 100, proteinG: 8, carbG: 15, fatG: 1, fiberG: 6, sugarG: 3, sodiumMg: 60, glycemicIndex: 30, typicalServingG: 150, costTier: 1, tags: ['legume', 'high-fiber', 'high-protein', 'onion'], allergens: [] },
  { id: 'peanuts', name: 'Roasted peanuts', locale: 'global', category: 'vegan', mealSlots: ['eveningsnack'], kcal: 567, proteinG: 26, carbG: 16, fatG: 49, fiberG: 8.5, sugarG: 4, sodiumMg: 18, typicalServingG: 30, costTier: 1, tags: ['nuts', 'healthy-fat', 'high-purine'], allergens: ['peanut'] },

  // ---- lunch / dinner staples ----
  { id: 'roti', name: 'Whole-wheat roti', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 264, proteinG: 9, carbG: 49, fatG: 4, fiberG: 8, sugarG: 1.5, sodiumMg: 190, glycemicIndex: 52, typicalServingG: 40, costTier: 1, tags: ['grain', 'high-fiber'], allergens: ['gluten'] },
  { id: 'brown-rice', name: 'Brown rice (cooked)', locale: 'global', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 123, proteinG: 2.7, carbG: 26, fatG: 1, fiberG: 1.8, sugarG: 0.4, sodiumMg: 4, glycemicIndex: 50, typicalServingG: 150, costTier: 1, tags: ['grain'], allergens: [] },
  { id: 'white-rice', name: 'White rice (cooked)', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 130, proteinG: 2.7, carbG: 28, fatG: 0.3, fiberG: 0.4, sugarG: 0.1, sodiumMg: 1, glycemicIndex: 73, typicalServingG: 150, costTier: 1, tags: ['grain', 'high-gi', 'refined'], allergens: [] },
  { id: 'dal-tadka', name: 'Dal tadka (toor dal, cooked)', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 120, proteinG: 7, carbG: 18, fatG: 2.5, fiberG: 5, sugarG: 1.5, sodiumMg: 300, glycemicIndex: 30, typicalServingG: 200, costTier: 1, tags: ['legume', 'high-protein', 'high-fiber', 'onion', 'garlic'], allergens: [] },
  { id: 'rajma', name: 'Rajma (kidney beans, cooked)', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 127, proteinG: 8.7, carbG: 22, fatG: 0.5, fiberG: 6.4, sugarG: 0.3, sodiumMg: 240, glycemicIndex: 29, typicalServingG: 200, costTier: 1, tags: ['legume', 'high-protein', 'high-fiber', 'onion', 'garlic'], allergens: [] },
  { id: 'paneer', name: 'Paneer (cottage cheese)', locale: 'IN', category: 'vegetarian', mealSlots: ['lunch', 'dinner', 'eveningsnack'], kcal: 265, proteinG: 18, carbG: 3.6, fatG: 20, fiberG: 0, sugarG: 2.6, sodiumMg: 22, typicalServingG: 100, costTier: 2, tags: ['dairy', 'high-protein', 'high-satfat'], allergens: ['milk'] },
  { id: 'tofu', name: 'Tofu (firm)', locale: 'global', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 144, proteinG: 17, carbG: 3, fatG: 9, fiberG: 2, sugarG: 0.6, sodiumMg: 14, typicalServingG: 100, costTier: 2, tags: ['soy', 'high-protein'], allergens: ['soy'] },
  { id: 'curd', name: 'Curd / yogurt (plain)', locale: 'IN', category: 'vegetarian', mealSlots: ['lunch', 'dinner', 'midmorning'], kcal: 60, proteinG: 3.5, carbG: 4.7, fatG: 3.3, fiberG: 0, sugarG: 4.7, sodiumMg: 46, typicalServingG: 150, costTier: 1, tags: ['dairy', 'probiotic'], allergens: ['milk'] },
  { id: 'mixed-veg-sabzi', name: 'Mixed vegetable sabzi', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 90, proteinG: 3, carbG: 12, fatG: 3.5, fiberG: 4.5, sugarG: 4, sodiumMg: 250, glycemicIndex: 35, typicalServingG: 150, costTier: 1, tags: ['vegetable', 'high-fiber', 'onion', 'garlic'], allergens: [] },
  { id: 'palak', name: 'Palak (spinach, cooked)', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 40, proteinG: 3, carbG: 4, fatG: 1.5, fiberG: 3, sugarG: 0.5, sodiumMg: 80, typicalServingG: 150, costTier: 1, tags: ['vegetable', 'high-fiber', 'high-potassium'], allergens: [] },
  { id: 'khichdi', name: 'Moong dal khichdi', locale: 'IN', category: 'vegan', mealSlots: ['lunch', 'dinner'], kcal: 120, proteinG: 5, carbG: 20, fatG: 2.5, fiberG: 3, sugarG: 1, sodiumMg: 260, glycemicIndex: 48, typicalServingG: 250, costTier: 1, tags: ['grain', 'legume', 'light'], allergens: [] },

  // ---- non-veg ----
  { id: 'chicken-breast', name: 'Grilled chicken breast', locale: 'global', category: 'nonveg', mealSlots: ['lunch', 'dinner'], kcal: 165, proteinG: 31, carbG: 0, fatG: 3.6, fiberG: 0, sugarG: 0, sodiumMg: 74, typicalServingG: 120, costTier: 2, tags: ['high-protein', 'lean', 'high-purine'], allergens: [] },
  { id: 'fish-curry', name: 'Fish curry (rohu)', locale: 'IN', region: 'East', category: 'nonveg', mealSlots: ['lunch', 'dinner'], kcal: 150, proteinG: 17, carbG: 4, fatG: 7, fiberG: 1, sugarG: 2, sodiumMg: 320, typicalServingG: 150, costTier: 2, tags: ['high-protein', 'omega3', 'onion', 'garlic', 'high-purine'], allergens: ['fish'] },
  { id: 'egg-curry', name: 'Egg curry', locale: 'IN', category: 'egg', mealSlots: ['lunch', 'dinner'], kcal: 180, proteinG: 11, carbG: 6, fatG: 12, fiberG: 1.5, sugarG: 3, sodiumMg: 350, typicalServingG: 180, costTier: 1, tags: ['high-protein', 'onion', 'garlic'], allergens: ['egg'] },

  // ---- bedtime ----
  { id: 'turmeric-milk', name: 'Turmeric milk (haldi doodh)', locale: 'IN', category: 'vegetarian', mealSlots: ['bedtime'], kcal: 70, proteinG: 3.3, carbG: 6, fatG: 3.5, fiberG: 0.2, sugarG: 6, sodiumMg: 45, typicalServingG: 200, costTier: 1, tags: ['dairy'], allergens: ['milk'] },
  { id: 'warm-milk', name: 'Warm milk (toned)', locale: 'IN', category: 'vegetarian', mealSlots: ['bedtime', 'wakeup'], kcal: 50, proteinG: 3.3, carbG: 5, fatG: 1.5, fiberG: 0, sugarG: 5, sodiumMg: 44, typicalServingG: 200, costTier: 1, tags: ['dairy'], allergens: ['milk'] },
];
