import { round } from '../../calc/anthropometry';
import type { DayPlan, FoodItem } from '../food/food.types';

export interface GroceryItem {
  foodId: string;
  name: string;
  totalGrams: number;
  servings: number;
  costTier: 1 | 2 | 3;
}

/**
 * Aggregates every meal item across the plan's days into a shopping list (total grams +
 * serving count per food). Pure. Optionally trims to a budget by dropping the priciest
 * tier-3 extras first (never core staples).
 */
export function buildGroceryList(
  days: DayPlan[],
  foods: FoodItem[],
  opts: { monthlyBudget?: number } = {},
): { items: GroceryItem[]; estimatedCostTierTotal: number; trimmed: string[] } {
  const byId = new Map(foods.map((f) => [f.id, f]));
  const grams = new Map<string, number>();

  for (const day of days) {
    for (const meal of day.meals) {
      for (const item of meal.items) {
        grams.set(item.foodId, (grams.get(item.foodId) ?? 0) + item.grams);
      }
    }
  }

  let items: GroceryItem[] = [...grams.entries()]
    .map(([foodId, totalGrams]) => {
      const food = byId.get(foodId);
      const serving = food?.typicalServingG ?? 100;
      return {
        foodId,
        name: food?.name ?? foodId,
        totalGrams: round(totalGrams, 0),
        servings: round(totalGrams / serving, 1),
        costTier: (food?.costTier ?? 2) as 1 | 2 | 3,
      };
    })
    .sort((a, b) => a.name.localeCompare(b.name));

  const trimmed: string[] = [];
  // Cost model: tier * grams/100 as arbitrary "cost points".
  const cost = (i: GroceryItem) => i.costTier * (i.totalGrams / 100);
  let total = round(items.reduce((s, i) => s + cost(i), 0), 1);

  // If a (unitless) budget is given, drop the priciest tier-3 items until under budget.
  if (opts.monthlyBudget && total > opts.monthlyBudget) {
    const droppable = items.filter((i) => i.costTier === 3).sort((a, b) => cost(b) - cost(a));
    for (const d of droppable) {
      if (total <= opts.monthlyBudget) break;
      items = items.filter((i) => i.foodId !== d.foodId);
      trimmed.push(d.name);
      total = round(total - cost(d), 1);
    }
  }

  return { items, estimatedCostTierTotal: total, trimmed };
}
