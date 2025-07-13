import { Recipe } from '@/lib/types/recipe';
import { Card, CardContent } from '@/components/ui/card';
import { ImageIcon, Utensils } from 'lucide-react';
import { cn } from '@/lib/utils';

interface RecipeSearchTileProps {
    recipe: Recipe;
    onClick: () => void;
}

export function RecipeSearchTile({ recipe, onClick }: RecipeSearchTileProps) {
    return (
        <Card
            className='cursor-pointer overflow-hidden transition-shadow hover:shadow-md'
            onClick={onClick}
        >
            <div className='flex gap-2 p-2'>
                {/* Recipe Image */}
                <div className='relative h-12 w-12 shrink-0 overflow-hidden rounded-md'>
                    {recipe.thumbnail?.base64String ? (
                        <img
                            src={`data:image/jpeg;base64,${recipe.thumbnail.base64String}`}
                            alt={recipe.title}
                            className='h-full w-full object-cover'
                        />
                    ) : (
                        <div className='flex h-full w-full items-center justify-center bg-gray-100'>
                            <ImageIcon className='size-4 text-gray-400' />
                        </div>
                    )}
                </div>

                {/* Recipe Info */}
                <div className='flex-1 min-w-0'>
                    <h3 className='font-medium text-xs text-gray-900 truncate mb-1'>
                        {recipe.title}
                    </h3>
                    <p className='text-xs text-gray-600 leading-3 line-clamp-2'>
                        {recipe.description || 'No description available'}
                    </p>
                    {recipe.tags && recipe.tags.length > 0 && (
                        <div className='flex gap-1 mt-1 flex-wrap'>
                            {recipe.tags.slice(0, 2).map((tag) => (
                                <span
                                    key={tag.id}
                                    className='inline-flex items-center rounded-full bg-gray-100 px-1.5 py-0.5 text-xs font-medium text-gray-800'
                                >
                                    {tag.name}
                                </span>
                            ))}
                            {recipe.tags.length > 2 && (
                                <span className='text-xs text-gray-500'>
                                    +{recipe.tags.length - 2}
                                </span>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </Card>
    );
} 