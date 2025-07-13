import { Recipe } from '@/lib/types/recipe';
import { RecipeSearchTile } from './recipe-search-tile';
import { useNavigate } from 'react-router-dom';

interface RecipeSearchResultsProps {
    recipes?: Recipe[];
    isLoading?: boolean;
}

export function RecipeSearchResults({ recipes, isLoading }: RecipeSearchResultsProps) {
    const navigate = useNavigate();

    if (isLoading) {
        return (
            <div className='mt-3 space-y-2 max-w-sm'>
                <div className='text-xs text-gray-500 mb-2'>Loading recipes...</div>
                {[...Array(3)].map((_, index) => (
                    <div
                        key={index}
                        className='animate-pulse'
                    >
                        <div className='flex gap-2 p-2 bg-gray-50 rounded-lg'>
                            <div className='h-12 w-12 bg-gray-200 rounded-md shrink-0'></div>
                            <div className='flex-1 space-y-1'>
                                <div className='h-3 bg-gray-200 rounded w-3/4'></div>
                                <div className='h-3 bg-gray-200 rounded w-full'></div>
                                <div className='flex gap-1 mt-1'>
                                    <div className='h-4 w-10 bg-gray-200 rounded-full'></div>
                                    <div className='h-4 w-12 bg-gray-200 rounded-full'></div>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    if (!recipes || recipes.length === 0) {
        return null;
    }

    return (
        <div className='mt-3 space-y-2 max-w-sm'>
            <div className='text-xs text-gray-500 mb-2'>
                {recipes.length} recipe{recipes.length !== 1 ? 's' : ''} found:
            </div>
            {recipes.map((recipe) => (
                <RecipeSearchTile
                    key={recipe.id}
                    recipe={recipe}
                    onClick={() => navigate(`/recipes/${recipe.id}`)}
                />
            ))}
        </div>
    );
} 