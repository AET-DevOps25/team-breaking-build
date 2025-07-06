'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Recipe } from '@/lib/types/recipe';
import { getRecipes } from '@/lib/services/mockRecipeService'; // Change it to original service when available
import { RecipeCard } from '@/components/recipe/recipe-card';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
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
      setRecipes((prev) => [...prev, ...response]);
      setHasMore(response.length > 0);
      setPage((prev) => prev + 1);
    } catch (error) {
      console.error('Error loading recipes:', error);
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

      <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
        {recipes.map((recipe) => (
          <RecipeCard
            key={recipe.id}
            recipe={recipe}
            onClick={() => router.push(`/recipes/${recipe.id}`)}
          />
        ))}
      </div>

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
