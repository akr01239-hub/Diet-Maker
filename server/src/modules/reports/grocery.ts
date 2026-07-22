import type { DayPlan, FoodItem } from '../food/food.types';
import { CATEGORY_ORDER, FOOD_INGREDIENTS } from '../../data/ingredients';

export interface GroceryLine {
  name: string;
  /** How many meals across the week use this ingredient (times to buy for). */
  meals: number;
  /** Approx amount to buy this week (rounded). */
  qty: number;
  /** Unit for qty: 'g' (grams) or 'pcs' (pieces, for eggs/fruit). */
  unit: 'g' | 'pcs';
}

/** Ingredients bought by the piece rather than by weight. */
const PIECE_ITEM = /\b(egg|banana|apple|orange|guava|lemon|mosambi)\b/i;

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
  const acc = new Map<string, { category: string; meals: number; grams: number }>();

  for (const day of days) {
    for (const meal of day.meals) {
      const seen = new Set<string>(); // count each ingredient at most once per meal
      for (const item of meal.items) {
        const ings =
          FOOD_INGREDIENTS[item.foodId] ??
          [{ name: byId.get(item.foodId)?.name ?? item.name, category: 'Other' }];
        // Split the cooked serving across the dish's raw ingredients as a shopping estimate.
        const perIng = item.grams / Math.max(1, ings.length);
        for (const ing of ings) {
          const cur = acc.get(ing.name) ?? { category: ing.category, meals: 0, grams: 0 };
          cur.grams += perIng;
          if (!seen.has(ing.name)) {
            cur.meals += 1;
            seen.add(ing.name);
          }
          acc.set(ing.name, cur);
        }
      }
    }
  }

  const catMap = new Map<string, GroceryLine[]>();
  for (const [name, { category, meals, grams }] of acc) {
    const piece = PIECE_ITEM.test(name);
    const line: GroceryLine = piece
      ? { name, meals, qty: meals, unit: 'pcs' } // ~1 piece per meal (egg, fruit)
      : { name, meals, qty: Math.max(5, Math.round(grams / 5) * 5), unit: 'g' };
    const arr = catMap.get(category) ?? [];
    arr.push(line);
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
