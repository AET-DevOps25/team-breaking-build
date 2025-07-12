import { useNavigate } from 'react-router-dom';
import { Recipe } from '@/lib/types/recipe';
import { getRecipes } from '@/lib/services/recipeService';
import { RecipeCard } from '@/components/recipe/recipe-card';
import { Button } from '@/components/ui/button';
import { InfiniteScroll } from '@/components/ui/infinite-scroll';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { Plus, ChefHat } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

export default function RecipesPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const {
    data: recipes,
    loading,
    hasMore,
    error,
    loadMore,
  } = useInfiniteScroll<Recipe>({
    fetchData: getRecipes,
    pageSize: 10,
    initialPage: 0,
  });

  return (
    <div className='container mx-auto px-4 py-8'>
      <div className='mb-8 flex items-center justify-between'>
        <h1 className='text-3xl font-bold'>Recipes</h1>
        {isAuthenticated && (
          <Button
            onClick={() => navigate('/recipes/create')}
            className='bg-[#FF7C75] hover:bg-[#FF7C75]/90'
          >
            <Plus className='mr-2 size-4' />
            Create Recipe
          </Button>
        )}
      </div>

      {error && (
        <div className='mb-6 rounded-lg bg-red-50 p-4 text-red-700'>
          <p>Error loading recipes: {error}</p>
        </div>
      )}

      {recipes.length > 0 ? (
        <InfiniteScroll
          onLoadMore={loadMore}
          hasMore={hasMore}
          loading={loading}
        >
          <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
            {recipes.slice().reverse().map((recipe) => {
              console.log('Recipe in list:', { id: recipe.id, title: recipe.title });
              return (
                <RecipeCard
                  key={recipe.id}
                  recipe={recipe}
                  onClick={() => navigate(`/recipes/${recipe.id}`)}
                />
              );
            })}
          </div>
        </InfiniteScroll>
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
    </div>
  );
} 