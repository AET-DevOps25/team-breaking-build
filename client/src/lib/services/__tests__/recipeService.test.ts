import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from '../../api';
import {
  getRecipes,
  getRecipeDetails,
  createRecipe,
  updateRecipe,
  deleteRecipe,
  getUserRecipes,
  getTags,
  Tag,
} from '../recipeService';
import { Recipe, RecipeMetadataDTO, BranchDTO, CommitDetailsResponse } from '../../types/recipe';

// Mock the api module
vi.mock('../../api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

// Mock the utils module
vi.mock('../../utils', () => ({
  decodeRecipeImageFromAPI: vi.fn((dto) => ({ base64String: dto?.base64String })),
  encodeRecipeImageForAPI: vi.fn((image) => ({ base64String: image?.base64String })),
}));

describe('Recipe Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getRecipes', () => {
    it('should fetch recipes with default pagination', async () => {
      const mockResponse = {
        content: [
          {
            id: 1,
            userId: 'user-123',
            title: 'Test Recipe',
            description: 'Test Description',
            servingSize: 4,
            createdAt: '2023-01-01T00:00:00Z',
            updatedAt: '2023-01-01T00:00:00Z',
            tags: [{ id: 1, name: 'Italian' }],
            thumbnail: { base64String: 'test-thumbnail' },
            forkedFrom: null,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      };

      (api.get as any).mockResolvedValue(mockResponse);

      const result = await getRecipes();

      expect(api.get).toHaveBeenCalledWith('/recipes?page=1&size=10');
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual(
        expect.objectContaining({
          id: 1,
          userId: 'user-123',
          title: 'Test Recipe',
          description: 'Test Description',
          servingSize: 4,
          tags: [{ id: 1, name: 'Italian' }],
        })
      );
    });

    it('should fetch recipes with custom pagination', async () => {
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 5,
        number: 2,
      };

      (api.get as any).mockResolvedValue(mockResponse);

      const result = await getRecipes(3, 5);

      expect(api.get).toHaveBeenCalledWith('/recipes?page=3&size=5');
      expect(result).toHaveLength(0);
    });

    it('should handle API errors', async () => {
      (api.get as any).mockRejectedValue(new Error('API Error'));

      await expect(getRecipes()).rejects.toThrow('API Error');
    });
  });

  describe('getRecipeDetails', () => {
    it('should get recipe details successfully', async () => {
      const mockBranches: BranchDTO[] = [
        {
          id: 1,
          name: 'main',
          recipeId: 1,
          headCommitId: 123,
          createdAt: '2023-01-01T00:00:00Z',
        },
      ];

      const mockCommitDetails: CommitDetailsResponse = {
        commitMetadata: {
          id: 1,
          userId: 'user-123',
          message: 'Initial commit',
          parentId: null,
          createdAt: '2023-01-01T00:00:00Z',
        },
        recipeDetails: {
          servingSize: 4,
          recipeIngredients: [
            { name: 'flour', amount: 2, unit: 'cups' },
          ],
          recipeSteps: [
            { order: 1, details: 'Mix ingredients' },
          ],
        },
      };

      (api.get as any)
        .mockResolvedValueOnce(mockBranches)
        .mockResolvedValueOnce(mockCommitDetails);

      const result = await getRecipeDetails(1);

      expect(api.get).toHaveBeenCalledWith('/vcs/recipes/1/branches');
      expect(api.get).toHaveBeenCalledWith('/vcs/commits/123');
      expect(result).toEqual(mockCommitDetails.recipeDetails);
    });

    it('should return null when no main branch found', async () => {
      const mockBranches: BranchDTO[] = [
        {
          id: 1,
          name: 'feature',
          recipeId: 1,
          headCommitId: 123,
          createdAt: '2023-01-01T00:00:00Z',
        },
      ];

      (api.get as any).mockResolvedValueOnce(mockBranches);

      const result = await getRecipeDetails(1);

      expect(result).toBeNull();
    });

    it('should handle API errors and throw them', async () => {
      (api.get as any).mockRejectedValue(new Error('Branch API Error'));

      await expect(getRecipeDetails(1)).rejects.toThrow('Branch API Error');
    });

    it('should handle errors and log them', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      
      (api.get as any).mockRejectedValue(new Error('Network Error'));

      await expect(getRecipeDetails(1)).rejects.toThrow('Network Error');
      
      expect(consoleSpy).toHaveBeenCalledWith('Failed to fetch recipe details:', expect.any(Error));
      
      consoleSpy.mockRestore();
    });
  });

  describe('createRecipe', () => {
    it('should create recipe successfully', async () => {
      const mockRecipeResponse = {
        id: 1,
        title: 'New Recipe',
        description: 'New Description',
        servingSize: 4,
        tags: [{ id: 1, name: 'Italian' }],
        thumbnail: { base64String: 'thumbnail-data' },
        userId: 'user-123',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z',
        forkedFrom: null,
      };

      const createRecipeRequest = {
        metadata: {
          title: 'New Recipe',
          description: 'New Description',
          servingSize: 4,
          thumbnail: { base64String: 'thumbnail-data' },
          tags: [{ id: 1, name: 'Italian' }],
        },
        initRequest: {
          recipeDetails: {
            servingSize: 4,
            recipeIngredients: [
              { name: 'flour', amount: 2, unit: 'cups' },
            ],
            recipeSteps: [
              { order: 1, details: 'Mix ingredients' },
            ],
          },
        },
      };

      (api.post as any).mockResolvedValue(mockRecipeResponse);

      const result = await createRecipe(createRecipeRequest);

      expect(api.post).toHaveBeenCalledWith('/recipes', createRecipeRequest);
      expect(result).toEqual(expect.objectContaining({
        id: 1,
        title: 'New Recipe',
        description: 'New Description',
      }));
    });

    it('should handle create recipe errors', async () => {
      const createRecipeRequest = {
        metadata: {
          title: 'New Recipe',
          description: 'New Description',
          servingSize: 4,
          tags: [],
        },
        initRequest: {
          recipeDetails: {
            servingSize: 4,
            recipeIngredients: [],
            recipeSteps: [],
          },
        },
      };

      (api.post as any).mockRejectedValue(new Error('Create Error'));

      await expect(createRecipe(createRecipeRequest)).rejects.toThrow('Create Error');
    });
  });

  describe('updateRecipe', () => {
    it('should update recipe successfully', async () => {
      const mockRecipeResponse = {
        id: 1,
        title: 'Updated Recipe',
        description: 'Updated Description',
        servingSize: 6,
        tags: [{ id: 1, name: 'Italian' }],
        thumbnail: { base64String: 'updated-thumbnail' },
        userId: 'user-123',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-02T00:00:00Z',
        forkedFrom: null,
      };

      const recipeMetadata = {
        title: 'Updated Recipe',
        description: 'Updated Description',
        servingSize: 6,
        tags: [{ id: 1, name: 'Italian' }],
        thumbnail: { base64String: 'updated-thumbnail' },
      };

      (api.put as any).mockResolvedValue(mockRecipeResponse);

      const result = await updateRecipe(1, recipeMetadata);

      expect(api.put).toHaveBeenCalledWith('/recipes/1', expect.objectContaining({
        title: 'Updated Recipe',
        description: 'Updated Description',
        servingSize: 6,
      }));
      expect(result).toEqual(expect.objectContaining({
        id: 1,
        title: 'Updated Recipe',
        description: 'Updated Description',
      }));
    });

    it('should handle update recipe errors', async () => {
      const mockRecipeData: RecipeMetadataDTO = {
        id: 1,
        title: 'Updated Recipe',
        description: 'Updated Description',
        servingSize: 6,
        tags: [],
        thumbnail: undefined,
        userId: 'user-123',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-02T00:00:00Z',
        forkedFrom: undefined,
      };

      (api.put as any).mockRejectedValue(new Error('Update Error'));

      await expect(updateRecipe(1, mockRecipeData)).rejects.toThrow('Update Error');
    });
  });

  describe('deleteRecipe', () => {
    it('should delete recipe successfully', async () => {
      (api.delete as any).mockResolvedValue(undefined);

      await deleteRecipe(1);

      expect(api.delete).toHaveBeenCalledWith('/recipes/1');
    });

    it('should handle delete recipe errors', async () => {
      (api.delete as any).mockRejectedValue(new Error('Delete Error'));

      await expect(deleteRecipe(1)).rejects.toThrow('Delete Error');
    });
  });

  describe('getUserRecipes', () => {
    it('should fetch user recipes with default pagination', async () => {
      const mockResponse = {
        content: [
          {
            id: 1,
            userId: 'user-123',
            title: 'User Recipe',
            description: 'User Description',
            servingSize: 2,
            createdAt: '2023-01-01T00:00:00Z',
            updatedAt: '2023-01-01T00:00:00Z',
            tags: [],
            thumbnail: null,
            forkedFrom: null,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      };

      (api.get as any).mockResolvedValue(mockResponse);

      const result = await getUserRecipes('user-123');

      expect(api.get).toHaveBeenCalledWith('/users/user-123/recipes?page=0&size=10');
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual(
        expect.objectContaining({
          id: 1,
          userId: 'user-123',
          title: 'User Recipe',
        })
      );
    });

    it('should fetch user recipes with custom pagination', async () => {
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 5,
        number: 1,
      };

      (api.get as any).mockResolvedValue(mockResponse);

      const result = await getUserRecipes('user-123', 1, 5);

      expect(api.get).toHaveBeenCalledWith('/users/user-123/recipes?page=1&size=5');
      expect(result).toHaveLength(0);
    });

    it('should handle getUserRecipes errors', async () => {
      (api.get as any).mockRejectedValue(new Error('User Recipes Error'));

      await expect(getUserRecipes('user-123')).rejects.toThrow('User Recipes Error');
    });
  });

  describe('getTags', () => {
    it('should fetch recipe tags successfully', async () => {
      const mockTags: Tag[] = [
        { id: 1, name: 'Italian' },
        { id: 2, name: 'Vegetarian' },
        { id: 3, name: 'Quick' },
      ];

      (api.get as any).mockResolvedValue(mockTags);

      const result = await getTags();

      expect(api.get).toHaveBeenCalledWith('/recipes/tags');
      expect(result).toEqual(mockTags);
    });

    it('should handle getTags errors', async () => {
      (api.get as any).mockRejectedValue(new Error('Tags Error'));

      await expect(getTags()).rejects.toThrow('Tags Error');
    });
  });


  describe('Edge cases and error handling', () => {
    it('should handle empty recipe list', async () => {
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 10,
        number: 0,
      };

      (api.get as any).mockResolvedValue(mockResponse);

      const result = await getRecipes();

      expect(result).toEqual([]);
    });

    it('should handle malformed API response', async () => {
      (api.get as any).mockResolvedValue(null);

      await expect(getRecipes()).rejects.toThrow();
    });

    it('should handle network errors in all functions', async () => {
      const networkError = new Error('Network Error');
      
      (api.get as any).mockRejectedValue(networkError);
      (api.post as any).mockRejectedValue(networkError);
      (api.put as any).mockRejectedValue(networkError);
      (api.delete as any).mockRejectedValue(networkError);

      await expect(getRecipes()).rejects.toThrow('Network Error');
      await expect(getUserRecipes('user-123')).rejects.toThrow('Network Error');
      await expect(getTags()).rejects.toThrow('Network Error');
      await expect(deleteRecipe(1)).rejects.toThrow('Network Error');
    });
  });
}); 