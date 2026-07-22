import type { DayPlan, FoodItem } from '../food/food.types';
import { CATEGORY_ORDER, FOOD_INGREDIENTS } from '../../data/ingredients';

export interface GroceryLine {
  name: string;
  /** How many meals across the week use this ingredient (a "buy more" hint). */
  meals: number;
}

export interface GroceryCategory {
  category: string;
  items: GroceryLine[];
}

export interface GroceryResult {
  categories: GroceryCategory[];
  totalItems: number;
}

/**
 * Turns the week's meal plan into a RAW-INGREDIENT shopping list grouped by aisle/category
 * (flour, potato, onion…) — not a list of cooked dishes. `meals` counts how many meals use
 * each ingredient so you know roughly how much to buy. Pure.
 */
export function buildGroceryList(days: DayPlan[], foods: FoodItem[]): GroceryResult {
  const byId = new Map(foods.map((f) => [f.id, f]));
  const acc = new Map<string, { category: string; meals: number }>();

  for (const day of days) {
    for (const meal of day.meals) {
      // Count each ingredient at most once per meal.
      const seen = new Set<string>();
      for (const item of meal.items) {
        const ings =
          FOOD_INGREDIENTS[item.foodId] ??
          [{ name: byId.get(item.foodId)?.name ?? item.name, category: 'Other' }];
        for (const ing of ings) {
          if (seen.has(ing.name)) continue;
          seen.add(ing.name);
          const cur = acc.get(ing.name);
          if (cur) cur.meals += 1;
          else acc.set(ing.name, { category: ing.category, meals: 1 });
        }
      }
    }
  }

  const catMap = new Map<string, GroceryLine[]>();
  for (const [name, { category, meals }] of acc) {
    const arr = catMap.get(category) ?? [];
    arr.push({ name, meals });
    catMap.set(category, arr);
  }

  const categories: GroceryCategory[] = CATEGORY_ORDER.filter((c) => catMap.has(c)).map(
    (category) => ({
      category,
      items: catMap
        .get(category)!
        .sort((a, b) => b.meals - a.meals || a.name.localeCompare(b.name)),
    }),
  );

  return { categories, totalItems: acc.size };
}
