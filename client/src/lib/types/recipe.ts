export interface RecipeImage {
  id: number;
  url: string;
}

export interface RecipeTag {
  id: number;
  name: string;
}

export interface Recipe {
  id: string;
  userId: number;
  forkedFrom?: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  description: string;
  thumbnail?: string;
  servingSize: number;
  tags: string[];
  ingredients: Ingredient[];
  steps: Step[];
}

export interface Ingredient {
  name: string;
  amount: number;
  unit: string;
}

export interface Step {
  order: number;
  details: string;
  image?: File;
}

export interface RecipeFormData {
  title: string;
  description: string;
  servingSize: number;
  tags: string[];
  thumbnail?: File;
  ingredients: Ingredient[];
  steps: Step[];
}

export interface RecipeResponse {
  recipes: Recipe[];
  total: number;
  page: number;
  pageSize: number;
}
