import { FoodItem, PlanPreferences } from './food.types';

/** Which food categories a diet type permits. */
function categoryAllowed(dietType: string, cat: FoodItem['category']): boolean {
  switch (dietType) {
    case 'vegan':
      return cat === 'vegan';
    case 'veg':
    case 'jain':
    case 'satvik':
    case 'mediterranean': // treat as veg-leaning by default here
      return cat === 'vegan' || cat === 'vegetarian';
    case 'eggetarian':
      return cat === 'vegan' || cat === 'vegetarian' || cat === 'egg';
    case 'nonveg':
    case 'keto':
    case 'lowcarb':
    case 'highprotein':
    case 'if':
    case 'custom':
    default:
      return true;
  }
}

/** Tags a diet type forbids (e.g. jain/satvik exclude onion & garlic). */
function forbiddenTags(dietType: string): string[] {
  if (dietType === 'jain') return ['onion', 'garlic', 'root'];
  if (dietType === 'satvik') return ['onion', 'garlic'];
  return [];
}

/** Tags a set of conditions wants to avoid. */
export function conditionAvoidTags(conditions: string[]): string[] {
  const avoid = new Set<string>();
  if (conditions.includes('diabetes')) {
    avoid.add('high-sugar');
    avoid.add('high-gi');
  }
  if (conditions.includes('fatty_liver')) {
    avoid.add('high-sugar');
    avoid.add('refined');
    avoid.add('alcohol');
  }
  if (conditions.includes('hypertension') || conditions.includes('heart_disease')) {
    avoid.add('high-sodium');
  }
  if (conditions.includes('heart_disease')) avoid.add('high-satfat');
  if (conditions.includes('gout')) avoid.add('high-purine');
  if (conditions.includes('kidney_disease')) {
    avoid.add('high-potassium');
    avoid.add('high-phosphorus');
  }
  return [...avoid];
}

const norm = (s: string) => s.toLowerCase().trim();

/**
 * Returns the foods a user may eat: diet-compatible, allergen-free, and not carrying any
 * condition-avoid tag. Deterministic and pure.
 */
export function eligibleFoods(foods: FoodItem[], prefs: PlanPreferences): FoodItem[] {
  const allergies = new Set(prefs.allergies.map(norm));
  const forbidden = new Set(forbiddenTags(prefs.dietType).map(norm));
  const avoid = new Set(conditionAvoidTags(prefs.conditions).map(norm));

  return foods.filter((f) => {
    if (!categoryAllowed(prefs.dietType, f.category)) return false;

    // Allergen exclusion — match against declared allergens AND the food name.
    for (const a of allergies) {
      if (a && (f.allergens.some((x) => norm(x) === a) || norm(f.name).includes(a))) {
        return false;
      }
    }

    const tags = f.tags.map(norm);
    if (tags.some((t) => forbidden.has(t))) return false;
    if (tags.some((t) => avoid.has(t))) return false;

    return true;
  });
}
