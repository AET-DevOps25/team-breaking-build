import { Recipe, RecipeResponse } from '@/lib/types/recipe';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function getRecipes(page: number = 1, pageSize: number = 6): Promise<RecipeResponse> {
  const response = await fetch(`${API_BASE_URL}/api/recipes?page=${page - 1}&size=${pageSize}`);

  if (!response.ok) {
    throw new Error('Failed to fetch recipes');
  }

  return response.json();
}

export async function createRecipe(data: FormData): Promise<Recipe> {
  const response = await fetch(`${API_BASE_URL}/api/recipes`, {
    method: 'POST',
    body: data,
  });

  if (!response.ok) {
    throw new Error('Failed to create recipe');
  }

  return response.json();
}

export async function updateRecipe(id: number, data: FormData): Promise<Recipe> {
  const response = await fetch(`${API_BASE_URL}/api/recipes/${id}`, {
    method: 'PUT',
    body: data,
  });

  if (!response.ok) {
    throw new Error('Failed to update recipe');
  }

  return response.json();
}

export async function deleteRecipe(id: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/recipes/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error('Failed to delete recipe');
  }
}

export async function getRecipe(id: number): Promise<Recipe> {
  const response = await fetch(`${API_BASE_URL}/api/recipes/${id}`);

  if (!response.ok) {
    throw new Error('Failed to fetch recipe');
  }

  return response.json();
}
