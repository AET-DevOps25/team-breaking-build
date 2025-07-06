'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Recipe } from '@/lib/types/recipe';
import { getRecipes } from '@/lib/services/recipeService';
import { RecipeCard } from '@/components/recipe/recipe-card';
import { Button } from '@/components/ui/button';
import { Plus, ChefHat } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

export default function RecipesPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const observerTarget = useRef<HTMLDivElement>(null);

  const loadRecipes = useCallback(async () => {
    if (loading || !hasMore) return;

    setLoading(true);
    try {
      const response = await getRecipes(page);

      // Ensure response is an array
      const recipesArray = Array.isArray(response) ? response : [];

      setRecipes((prev) => [...prev, ...recipesArray]);
      setHasMore(recipesArray.length > 0);
      setPage((prev) => prev + 1);
    } catch (error) {
      console.error('Error loading recipes:', error);
      setHasMore(false); // Stop trying to load more on error
    } finally {
      setLoading(false);
    }
  }, [loading, hasMore, page]);

  useEffect(() => {
    loadRecipes();
  }, [loadRecipes]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loading) {
          loadRecipes();
        }
      },
      { threshold: 0.1 },
    );

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [loadRecipes, hasMore, loading]);

  return (
    <div className='container mx-auto px-4 py-8'>
      <div className='mb-8 flex items-center justify-between'>
        <h1 className='text-3xl font-bold'>Recipes</h1>
        {isAuthenticated && (
          <Button
            onClick={() => router.push('/recipes/create')}
            className='bg-[#FF7C75] hover:bg-[#FF7C75]/90'
          >
            <Plus className='mr-2 size-4' />
            Create Recipe
          </Button>
        )}
      </div>

      {recipes.length > 0 ? (
        <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
          {recipes.map((recipe) => (
            <RecipeCard
              key={recipe.id}
              recipe={recipe}
              onClick={() => router.push(`/recipes/${recipe.id}`)}
            />
          ))}
        </div>
      ) : !loading ? (
        <div className='flex flex-col items-center justify-center py-12'>
          <div className='mb-6 flex size-24 items-center justify-center rounded-full bg-rose-100'>
            <ChefHat className='size-12 text-[#FF7C75]' />
          </div>
          <div className='mb-4 text-center'>
            <h3 className='mb-2 text-xl font-semibold text-gray-900'>No recipes available</h3>
            <p className='text-gray-600'>Be the first to create a recipe and share it with the community!</p>
          </div>
        </div>
      ) : null}

      {/* Loading indicator and intersection observer target */}
      <div
        ref={observerTarget}
        className='flex h-10 items-center justify-center'
      >
        {loading && <div className='size-8 animate-spin rounded-full border-b-2 border-[#FF7C75]'></div>}
      </div>
    </div>
  );
}
