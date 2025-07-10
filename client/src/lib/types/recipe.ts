export interface RecipeImage {
  base64String?: string; // nullable, can be undefined/null if no image is provided
}

// API types - these match the backend structure where images are actually base64 strings
export interface RecipeImageDTO {
  base64String?: string; // base64 string from backend (same as client format)
}

export interface RecipeTag {
  id: number;
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
  recipeImageDTOS?: RecipeImage[]; // optional, can be undefined/null/empty
}

export interface RecipeDetails {
  servingSize: number;
  images?: RecipeImage[]; // optional, can be undefined/null/empty
  recipeIngredients: RecipeIngredient[];
  recipeSteps: RecipeStep[];
}

export interface RecipeMetadata {
  id?: number;
  userId?: string;
  forkedFrom?: number;
  createdAt?: string;
  updatedAt?: string;
  title: string;
  description: string;
  thumbnail?: RecipeImage; // now uses RecipeImage instead of string
  servingSize: number;
  tags?: RecipeTag[]; // optional, can be undefined/null/empty, now full tag objects
}

// API types for backend communication
export interface RecipeMetadataDTO {
  id?: number;
  userId?: string;
  forkedFrom?: number;
  createdAt?: string;
  updatedAt?: string;
  title: string;
  description: string;
  thumbnail?: RecipeImageDTO; // backend expects RecipeImageDTO with byte array
  servingSize: number;
  tags?: RecipeTag[];
}

export interface CreateRecipeRequest {
  metadata: Omit<RecipeMetadata, 'tags'> & { tags?: RecipeTag[] };
  initRequest: {
    recipeDetails: RecipeDetails;
  };
}

// API request type for backend
export interface CreateRecipeRequestDTO {
  metadata: RecipeMetadataDTO;
  initRequest: {
    recipeDetails: RecipeDetails;
  };
}

export interface Recipe {
  id: number;
  userId?: string;
  forkedFrom?: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  description: string;
  thumbnail?: RecipeImage;
  servingSize: number;
  tags: RecipeTag[];
  // Optionally, details can be fetched separately
}

export interface RecipeResponse {
  recipes: Recipe[];
  total: number;
  page: number;
  pageSize: number;
}
