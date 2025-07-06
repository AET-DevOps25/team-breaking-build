import { Recipe } from '@/lib/types/recipe';
import { api } from '@/lib/api';

// Define interfaces for the parsed JSON data
interface ParsedIngredient {
  name: string;
  unit: string;
  amount: number;
}

interface ParsedStep {
  order: number;
  details: string;
}

export interface Tag {
  id: number;
  name: string;
}

export async function getRecipes(page: number = 1, limit: number = 10): Promise<Recipe[]> {
  try {
    const response = await api.get<Recipe[]>(`/recipes?page=${page}&limit=${limit}`);
    return response;
  } catch (error) {
    console.error('Error fetching recipes:', error);
    throw error;
  }
}

export async function getRecipe(id: string): Promise<Recipe | null> {
  try {
    const response = await api.get<Recipe>(`/recipes/${id}`);
    return response;
  } catch (error) {
    console.error('Error fetching recipe:', error);
    throw error;
  }
}

export async function getTags(): Promise<Tag[]> {
  try {
    const response = await api.get<Tag[]>('/recipes/tags');
    return response;
  } catch (error) {
    console.error('Error fetching tags:', error);
    throw error;
  }
}

export async function createRecipe(data: FormData): Promise<Recipe> {
  try {
    const createRequest = {
      metadata: {
        title: data.get('title') as string,
        description: data.get('description') as string,
        servingSize: parseInt(data.get('servingSize') as string),
        tags: (data.getAll('tags') as string[]).map((tag) => ({ name: tag })),
        thumbnail: data.get('thumbnail') ? { url: 'placeholder-url' } : undefined,
      },
      initRequest: {
        recipeDetails: {
          servingSize: parseInt(data.get('servingSize') as string),
          recipeIngredients: JSON.parse(data.get('ingredients') as string).map((ing: ParsedIngredient) => ({
            name: ing.name,
            unit: ing.unit,
            amount: ing.amount,
          })),
          recipeSteps: JSON.parse(data.get('steps') as string).map((step: ParsedStep) => ({
            order: step.order,
            details: step.details,
          })),
        },
      },
    };

    const response = await api.post<Recipe>('/recipes', createRequest);
    return response;
  } catch (error) {
    console.error('Error creating recipe:', error);
    throw error;
  }
}

export async function updateRecipe(id: string, data: FormData): Promise<Recipe | null> {
  try {
    const response = await api.put<Recipe>(`/recipes/${id}`, data);
    return response;
  } catch (error) {
    console.error('Error updating recipe:', error);
    throw error;
  }
}

export async function deleteRecipe(id: string): Promise<void> {
  try {
    await api.delete(`/recipes/${id}`);
  } catch (error) {
    console.error('Error deleting recipe:', error);
    throw error;
  }
}

export async function getUserRecipes(userId: string): Promise<Recipe[]> {
  try {
    const response = await api.get<Recipe[]>(`/recipes/user/${userId}`);
    return response;
  } catch (error) {
    console.error('Error fetching user recipes:', error);
    throw error;
  }
}

export async function copyRecipe(id: string, userId: number, branchId: number): Promise<Recipe | null> {
  try {
    // The server expects userId and branchId as query parameters
    const response = await api.post<Recipe>(`/recipes/${id}/copy?userId=${userId}&branchId=${branchId}`, {});
    return response;
  } catch (error) {
    console.error('Error copying recipe:', error);
    throw error;
  }
}
