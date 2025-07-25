import { Recipe, RecipeMetadata, CreateRecipeRequest, RecipeMetadataDTO, BranchDTO, CommitDTO, CommitDetailsResponse, ChangeResponse } from '@/lib/types/recipe';
import { api } from '@/lib/api';
import { encodeRecipeImageForAPI, decodeRecipeImageFromAPI } from '@/lib/utils';

export interface Tag {
  id: number;
  name: string;
}

interface RecipeAPIResponse {
  id: number;
  userId?: string;
  forkedFrom?: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  description: string;
  thumbnail?: { base64String?: string };
  servingSize: number;
  tags: Tag[];
}

interface PaginatedRecipeResponse {
  content: RecipeAPIResponse[];
}





// Helper function to convert API Recipe to client format
function convertRecipeFromAPI(apiRecipe: RecipeAPIResponse): Recipe {
  return {
    ...apiRecipe,
    thumbnail: apiRecipe.thumbnail ? decodeRecipeImageFromAPI(apiRecipe.thumbnail) : undefined,
  };
}

// Helper function to convert CreateRecipeRequest to API format
function convertCreateRequestToAPI(request: CreateRecipeRequest): CreateRecipeRequest {
  const metadataDTO = {
    ...request.metadata,
    thumbnail: request.metadata.thumbnail ? encodeRecipeImageForAPI(request.metadata.thumbnail) : undefined,
  };

  // Convert step images to API format
  const recipeSteps = request.initRequest.recipeDetails.recipeSteps.map((step) => ({
    order: step.order,
    details: step.details,
    recipeImageDTOS: step.recipeImageDTOS?.map((image) => encodeRecipeImageForAPI(image)),
  }));

  // Convert detail images to API format
  const images = request.initRequest.recipeDetails.images?.map((image) => encodeRecipeImageForAPI(image));

  return {
    metadata: metadataDTO,
    initRequest: {
      recipeDetails: {
        servingSize: request.initRequest.recipeDetails.servingSize,
        recipeIngredients: request.initRequest.recipeDetails.recipeIngredients,
        images,
        recipeSteps,
      },
    },
  };
}

// Helper function to convert RecipeMetadata to API format
function convertMetadataToAPI(metadata: RecipeMetadata): RecipeMetadataDTO {
  return {
    ...metadata,
    thumbnail: metadata.thumbnail ? encodeRecipeImageForAPI(metadata.thumbnail) : undefined,
  };
}

export async function getRecipes(page: number = 1, limit: number = 10): Promise<Recipe[]> {
  try {
    // Backend expects pagination via Pageable
    const response = await api.get<PaginatedRecipeResponse>(`/recipes?page=${page}&size=${limit}`);
    return response.content.map(convertRecipeFromAPI);
  } catch (error) {
    throw error;
  }
}

export async function getRecipe(id: number): Promise<Recipe | null> {
  try {
    const response = await api.get<RecipeAPIResponse>(`/recipes/${id}`);
    return convertRecipeFromAPI(response);
  } catch (error) {
    throw error;
  }
}

export async function getRecipesByIds(ids: number[]): Promise<Recipe[]> {
  try {
    if (ids.length === 0) {
      return [];
    }
    
    const idsParam = ids.join(',');
    const response = await api.get<RecipeAPIResponse[]>(`/recipes/batch?ids=${idsParam}`);
    return response.map(convertRecipeFromAPI);
  } catch (error) {
    console.error('Error fetching recipes by IDs:', error);
    return [];
  }
}

export async function getRecipeDetails(recipeId: number): Promise<CommitDetailsResponse['recipeDetails'] | null> {
  try {
    // First, get the branches for the recipe
    const branches = await api.get<BranchDTO[]>(`/vcs/recipes/${recipeId}/branches`);

    // Find the main branch
    const mainBranch = branches.find((branch) => branch.name === 'main');
    if (!mainBranch) {
      console.warn('No main branch found for recipe:', recipeId);
      return null;
    }

    // Get the commit details using the head commit ID
    const commitDetails = await api.get<CommitDetailsResponse>(`/vcs/commits/${mainBranch.headCommitId}`);

    return commitDetails.recipeDetails;
  } catch (error) {
    console.error('Failed to fetch recipe details:', error);
    throw error;
  }
}

export async function getRecipeBranches(recipeId: number): Promise<BranchDTO[]> {
  try {
    const response = await api.get<BranchDTO[]>(`/vcs/recipes/${recipeId}/branches`);
    return response;
  } catch (error) {
    throw error;
  }
}

export async function getTags(): Promise<Tag[]> {
  try {
    const response = await api.get<Tag[]>('/recipes/tags');
    return response;
  } catch (error) {
    throw error;
  }
}

export async function createRecipe(data: CreateRecipeRequest): Promise<Recipe> {
  try {
    // Convert to API format before sending
    const apiRequest = convertCreateRequestToAPI(data);
    const response = await api.post<RecipeAPIResponse>('/recipes', apiRequest);
    return convertRecipeFromAPI(response);
  } catch (error) {
    throw error;
  }
}

export async function updateRecipe(id: number, metadata: RecipeMetadata): Promise<Recipe | null> {
  try {
    // Convert metadata to API format before sending
    const apiMetadata = convertMetadataToAPI(metadata);
    const response = await api.put<RecipeAPIResponse>(`/recipes/${id}`, apiMetadata);
    return convertRecipeFromAPI(response);
  } catch (error) {
    throw error;
  }
}

export async function deleteRecipe(id: number): Promise<void> {
  try {
    await api.delete(`/recipes/${id}`);
  } catch (error) {
    throw error;
  }
}

export async function getUserRecipes(userId: string, page: number = 0, limit: number = 10): Promise<Recipe[]> {
  try {
    // Correct endpoint: /users/{userId}/recipes
    const response = await api.get<PaginatedRecipeResponse>(`/users/${userId}/recipes?page=${page}&size=${limit}`);
    return response.content.map(convertRecipeFromAPI);
  } catch (error) {
    throw error;
  }
}

export async function copyRecipe(recipeId: number, branchId: number): Promise<Recipe | null> {
  try {
    // userId is extracted from header, not as query param
    const response = await api.post<RecipeAPIResponse>(`/recipes/${recipeId}/copy?branchId=${branchId}`);
    return convertRecipeFromAPI(response);
  } catch (error) {
    throw error;
  }
}

export async function commitToBranch(
  branchId: number,
  message: string,
  recipeDetails: CommitDetailsResponse['recipeDetails'],
): Promise<CommitDTO> {
  try {
    const response = await api.post<CommitDTO>(`/vcs/branches/${branchId}/commit`, {
      message,
      recipeDetails,
    });
    return response;
  } catch (error) {
    throw error;
  }
}

export async function createBranch(
  recipeId: number,
  branchName: string,
  sourceBranchId: number,
): Promise<BranchDTO> {
  try {
    const response = await api.post<BranchDTO>(`/vcs/recipes/${recipeId}/branches`, {
      branchName,
      sourceBranchId,
    });
    return response;
  } catch (error) {
    throw error;
  }
}

export async function getBranchHistory(branchId: number): Promise<CommitDTO[]> {
  try {
    const response = await api.get<CommitDTO[]>(`/vcs/branches/${branchId}/history`);
    return response;
  } catch (error) {
    throw error;
  }
}

export async function getCommitDetails(commitId: number): Promise<CommitDetailsResponse> {
  try {
    const response = await api.get<CommitDetailsResponse>(`/vcs/commits/${commitId}`);
    return response;
  } catch (error) {
    throw error;
  }
}

export async function getCommitChanges(commitId: number): Promise<ChangeResponse> {
  try {
    const response = await api.get<ChangeResponse>(`/vcs/commits/${commitId}/changes`);
    return response;
  } catch (error) {
    throw error;
  }
}
