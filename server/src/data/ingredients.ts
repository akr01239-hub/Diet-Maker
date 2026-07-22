/**
 * Maps each cooked dish (food id) to the RAW ingredients you actually buy, with a shopping
 * category. This turns the meal plan into a real grocery list (flour, potato, onion) instead
 * of listing prepared dishes ("Aloo paratha"). Foods not listed fall back to their own name.
 */
export interface Ingredient {
  name: string;
  category: string;
}

const G = 'Grains & Flours';
const PU = 'Pulses & Lentils';
const V = 'Vegetables';
const F = 'Fruits';
const DE = 'Dairy & Eggs';
const MF = 'Meat & Fish';
const NS = 'Nuts & Seeds';
const P = 'Pantry & Spices';
const B = 'Beverages';

export const FOOD_INGREDIENTS: Record<string, Ingredient[]> = {
  'soaked-almonds': [{ name: 'Almonds', category: NS }],
  'green-tea': [{ name: 'Green tea', category: B }],
  poha: [{ name: 'Poha (flattened rice)', category: G }, { name: 'Onion', category: V }, { name: 'Peanuts', category: NS }, { name: 'Cooking oil', category: P }],
  idli: [{ name: 'Idli rice', category: G }, { name: 'Urad dal', category: PU }],
  'oats-porridge': [{ name: 'Oats', category: G }, { name: 'Milk', category: DE }],
  'boiled-egg': [{ name: 'Eggs', category: DE }],
  'moong-chilla': [{ name: 'Moong dal', category: PU }, { name: 'Onion', category: V }],
  'aloo-paratha': [{ name: 'Whole-wheat flour', category: G }, { name: 'Potato', category: V }, { name: 'Cooking oil', category: P }],
  banana: [{ name: 'Banana', category: F }],
  apple: [{ name: 'Apple', category: F }],
  buttermilk: [{ name: 'Curd / yogurt', category: DE }],
  'roasted-chana': [{ name: 'Roasted chana', category: PU }],
  'sprouts-salad': [{ name: 'Moong (for sprouts)', category: PU }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }],
  peanuts: [{ name: 'Peanuts', category: NS }],
  roti: [{ name: 'Whole-wheat flour', category: G }],
  'brown-rice': [{ name: 'Brown rice', category: G }],
  'white-rice': [{ name: 'Rice', category: G }],
  'dal-tadka': [{ name: 'Toor dal', category: PU }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }, { name: 'Cooking oil', category: P }, { name: 'Spices', category: P }],
  rajma: [{ name: 'Rajma (kidney beans)', category: PU }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }, { name: 'Spices', category: P }],
  paneer: [{ name: 'Paneer', category: DE }],
  tofu: [{ name: 'Tofu', category: DE }],
  curd: [{ name: 'Curd / yogurt', category: DE }],
  'mixed-veg-sabzi': [{ name: 'Mixed vegetables', category: V }, { name: 'Onion', category: V }, { name: 'Cooking oil', category: P }, { name: 'Spices', category: P }],
  palak: [{ name: 'Spinach', category: V }],
  khichdi: [{ name: 'Rice', category: G }, { name: 'Moong dal', category: PU }],
  'chicken-breast': [{ name: 'Chicken breast', category: MF }],
  'fish-curry': [{ name: 'Fish (rohu)', category: MF }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }, { name: 'Spices', category: P }],
  'egg-curry': [{ name: 'Eggs', category: DE }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }, { name: 'Spices', category: P }],
  'turmeric-milk': [{ name: 'Milk', category: DE }, { name: 'Turmeric', category: P }],
  'warm-milk': [{ name: 'Milk', category: DE }],
  upma: [{ name: 'Semolina (rava)', category: G }, { name: 'Onion', category: V }, { name: 'Mixed vegetables', category: V }, { name: 'Cooking oil', category: P }],
  'masala-dosa': [{ name: 'Idli rice', category: G }, { name: 'Urad dal', category: PU }, { name: 'Potato', category: V }],
  'besan-chilla': [{ name: 'Besan (gram flour)', category: PU }, { name: 'Onion', category: V }],
  daliya: [{ name: 'Broken wheat (daliya)', category: G }, { name: 'Mixed vegetables', category: V }],
  'jeera-rice': [{ name: 'Rice', category: G }, { name: 'Cumin seeds', category: P }],
  quinoa: [{ name: 'Quinoa', category: G }],
  'bajra-roti': [{ name: 'Bajra flour', category: G }],
  chole: [{ name: 'Chickpeas (chole)', category: PU }, { name: 'Onion', category: V }, { name: 'Tomato', category: V }, { name: 'Spices', category: P }],
  sambar: [{ name: 'Toor dal', category: PU }, { name: 'Mixed vegetables', category: V }, { name: 'Sambar powder', category: P }],
  'veg-pulao': [{ name: 'Rice', category: G }, { name: 'Mixed vegetables', category: V }],
  kadhi: [{ name: 'Besan (gram flour)', category: PU }, { name: 'Curd / yogurt', category: DE }],
  'chicken-tikka': [{ name: 'Chicken', category: MF }, { name: 'Curd / yogurt', category: DE }, { name: 'Spices', category: P }],
  'egg-white-omelette': [{ name: 'Eggs', category: DE }, { name: 'Onion', category: V }],
  'grilled-salmon': [{ name: 'Salmon', category: MF }],
  'greek-yogurt': [{ name: 'Greek yogurt', category: DE }],
  'cucumber-salad': [{ name: 'Cucumber', category: V }, { name: 'Tomato', category: V }],
  orange: [{ name: 'Orange', category: F }],
  guava: [{ name: 'Guava', category: F }],
  walnuts: [{ name: 'Walnuts', category: NS }],
  'coconut-water': [{ name: 'Coconut water', category: B }],
};

/** Preferred display order of categories. */
export const CATEGORY_ORDER = [G, PU, V, F, DE, MF, NS, B, P, 'Other'];
