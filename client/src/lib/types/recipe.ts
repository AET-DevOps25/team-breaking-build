export interface RecipeImage {
  url: string;
}

export interface RecipeTag {
  id?: number;
  name: string;
}

export interface RecipeIngredient {
  name: string;
  unit: string;
  amount: number;
}

export interface RecipeStep {
  order: number;
  details: string;
  recipeImageDTOS?: RecipeImage[];
}

export interface RecipeDetails {
  servingSize: number;
  images?: RecipeImage[];
  recipeIngredients: RecipeIngredient[];
  recipeSteps: RecipeStep[];
}

export interface RecipeMetadata {
  id?: number;
  userId?: number;
  forkedFrom?: number;
  createdAt?: string;
  updatedAt?: string;
  title: string;
  description: string;
  thumbnail?: RecipeImage;
  servingSize: number;
  tags: RecipeTag[];
}

export interface CreateRecipeRequest {
  metadata: RecipeMetadata;
  initRequest: {
    userId?: number; // Will be handled by gateway
    recipeDetails: RecipeDetails;
  };
}

// For API responses and display
export interface Recipe {
  id: string;
  userId: number;
  forkedFrom?: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  description: string;
  thumbnail?: RecipeImage;
  servingSize: number;
  tags: RecipeTag[];
  // These might come from a separate API call to get recipe details
  ingredients?: RecipeIngredient[];
  steps?: RecipeStep[];
}

export interface RecipeResponse {
  recipes: Recipe[];
  total: number;
  page: number;
  pageSize: number;
}
