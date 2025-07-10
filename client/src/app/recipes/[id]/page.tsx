'use client';

import { useParams, useRouter } from 'next/navigation';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getRecipe, getRecipeDetails, deleteRecipe, copyRecipe, getRecipeBranches } from '@/lib/services/recipeService';
import { Recipe, RecipeIngredient, RecipeStep } from '@/lib/types/recipe';
import { RecipeDetailView } from '@/components/recipe/recipe-detail-view';
import { RecipeActionButtons } from '@/components/recipe/recipe-action-buttons';
import { useAuth } from '@/contexts/AuthContext';

interface RecipeDetails {
  servingSize: number;
  recipeIngredients: RecipeIngredient[];
  recipeSteps: RecipeStep[];
}

export default function RecipeDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [recipeDetails, setRecipeDetails] = useState<RecipeDetails | null>(null);
  const [originalRecipe, setOriginalRecipe] = useState<Recipe | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDetails, setIsLoadingDetails] = useState(true);
  const [recipeId, setRecipeId] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isCloning, setIsCloning] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  // Extract recipe ID from params
  useEffect(() => {
    if (params?.id) {
      const recipeIdParam = params.id;
      const extractedId =
        typeof recipeIdParam === 'string'
          ? Number(recipeIdParam)
          : Array.isArray(recipeIdParam)
            ? Number(recipeIdParam[0])
            : Number(recipeIdParam);

      console.log('Params:', params);
      console.log('Recipe ID Param:', recipeIdParam);
      console.log('Recipe ID (extracted):', extractedId);

      setRecipeId(extractedId);
    }
  }, [params]);

  // Handle invalid recipe ID
  useEffect(() => {
    if (recipeId !== null && isNaN(recipeId)) {
      toast.error('Invalid recipe ID');
      router.push('/recipes');
    }
  }, [recipeId, router]);

  useEffect(() => {
    if (!recipeId || isNaN(recipeId)) {
      return; // Don't redirect immediately, wait for recipeId to be set
    }

    const fetchRecipe = async () => {
      try {
        setIsLoading(true);
        const recipeData = await getRecipe(recipeId);
        setRecipe(recipeData);

        // If this is a forked recipe, fetch the original recipe
        if (recipeData && recipeData.forkedFrom) {
          await fetchOriginalRecipe(recipeData.forkedFrom);
        }
      } catch (error) {
        console.error('Failed to fetch recipe:', error);
        toast.error('Failed to load recipe');
        router.push('/recipes');
      } finally {
        setIsLoading(false);
      }
    };

    const fetchRecipeDetails = async () => {
      try {
        setIsLoadingDetails(true);
        const details = await getRecipeDetails(recipeId);
        if (details) {
          setRecipeDetails(details);
        }
      } catch (error) {
        console.error('Failed to fetch recipe details:', error);
        toast.error('Failed to load recipe details');
      } finally {
        setIsLoadingDetails(false);
      }
    };

    const fetchOriginalRecipe = async (forkedFromId: number) => {
      try {
        const original = await getRecipe(forkedFromId);
        setOriginalRecipe(original);
      } catch (error) {
        console.error('Failed to fetch original recipe:', error);
        // Don't show error toast for this as it's not critical
      }
    };

    fetchRecipe();
    fetchRecipeDetails();
  }, [recipeId, router]);

  // Fix ownership check - compare user.id with recipe.userId
  const isOwner = Boolean(user && recipe && user.id === recipe.userId);

  const handleCopy = async () => {
    if (!recipe || !user) {
      toast.error('You must be logged in to clone a recipe');
      return;
    }

    try {
      setIsCloning(true);

      // Fetch the branches for this recipe to get the main branch ID
      const branches = await getRecipeBranches(recipe.id);
      const mainBranch = branches.find((branch) => branch.name === 'main');

      if (!mainBranch) {
        toast.error('No main branch found for this recipe');
        return;
      }

      const clonedRecipe = await copyRecipe(recipe.id, mainBranch.id);

      if (clonedRecipe) {
        toast.success('Recipe successfully cloned!');
        // Navigate to the cloned recipe's detail page
        router.push(`/recipes/${clonedRecipe.id}`);
      } else {
        toast.error('Failed to clone recipe');
      }
    } catch (error) {
      console.error('Failed to clone recipe:', error);
      toast.error('Failed to clone recipe. Please try again.');
    } finally {
      setIsCloning(false);
    }
  };

  const handleEdit = () => {
    router.push(`/recipes/${recipeId}/edit`);
  };

  const handleDelete = async () => {
    if (!recipe || !user) {
      toast.error('You must be logged in to delete a recipe');
      return;
    }
    setShowDeleteDialog(true);
  };

  const confirmDelete = async () => {
    if (!recipe) {
      setShowDeleteDialog(false);
      return;
    }
    try {
      setIsDeleting(true);
      await deleteRecipe(recipe.id);
      toast.success('Recipe successfully deleted!');
      router.push('/recipes');
    } catch (error) {
      console.error('Failed to delete recipe:', error);
      toast.error('Failed to delete recipe. Please try again.');
    } finally {
      setIsDeleting(false);
      setShowDeleteDialog(false);
    }
  };

  if (isLoading || !recipeId || isNaN(recipeId)) {
    return (
      <div className='container mx-auto px-4 py-8'>
        <div className='mx-auto max-w-4xl'>
          <div className='animate-pulse'>
            <div className='mb-4 h-8 w-1/3 rounded bg-gray-200'></div>
            <div className='mb-8 h-4 w-1/2 rounded bg-gray-200'></div>
            <div className='mb-8 h-64 rounded bg-gray-200'></div>
            <div className='space-y-4'>
              <div className='h-4 w-3/4 rounded bg-gray-200'></div>
              <div className='h-4 w-1/2 rounded bg-gray-200'></div>
              <div className='h-4 w-5/6 rounded bg-gray-200'></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!recipe) {
    return (
      <div className='container mx-auto px-4 py-8'>
        <div className='mx-auto max-w-4xl text-center'>
          <h1 className='mb-4 text-2xl font-bold text-gray-900'>Recipe Not Found</h1>
          <p className='mb-8 text-gray-600'>
            The recipe you&apos;re looking for doesn&apos;t exist or has been removed.
          </p>
          <button
            onClick={() => router.push('/recipes')}
            className='rounded-lg bg-[#FF7C75] px-6 py-2 text-white transition-colors hover:bg-[#FF7C75]/90'
          >
            Back to Recipes
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className='container mx-auto px-4 py-8'>
      <div className='mx-auto max-w-4xl'>
        <RecipeActionButtons
          isOwner={isOwner}
          onCopy={handleCopy}
          onEdit={handleEdit}
          onDelete={handleDelete}
          isDeleting={isDeleting}
          isCloning={isCloning}
        />

        <RecipeDetailView
          recipe={recipe}
          recipeDetails={recipeDetails}
          isLoadingDetails={isLoadingDetails}
          originalRecipe={originalRecipe}
        />
      </div>
      {/* Delete Confirmation Dialog */}
      {showDeleteDialog && (
        <div className='fixed inset-0 z-50 flex items-center justify-center'>
          <div
            className='fixed inset-0 bg-black/50 backdrop-blur-sm'
            onClick={() => setShowDeleteDialog(false)}
          />
          <div className='relative z-50 m-4 w-full max-w-lg rounded-lg bg-white p-6 shadow-lg'>
            <div className='flex flex-col space-y-1.5 text-center sm:text-left'>
              <h2 className='text-lg font-semibold leading-none tracking-tight'>Confirm Delete</h2>
              <p className='mt-2 text-sm text-gray-600'>
                Are you sure you want to delete this recipe? This action cannot be undone.
              </p>
            </div>
            <div className='mt-6 flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2'>
              <button
                type='button'
                onClick={() => setShowDeleteDialog(false)}
                className='rounded-lg border border-gray-300 px-4 py-2 font-semibold text-gray-700 transition-colors hover:bg-gray-50 hover:text-gray-900'
                disabled={isDeleting}
              >
                Cancel
              </button>
              <button
                type='button'
                onClick={confirmDelete}
                className='rounded-lg bg-[#FF7C75] px-4 py-2 font-semibold text-white transition-colors hover:bg-rose-600'
                disabled={isDeleting}
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
