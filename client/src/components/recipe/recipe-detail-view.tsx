'use client';

import { Recipe, RecipeIngredient, RecipeStep } from '@/lib/types/recipe';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import Image from 'next/image';
import { Clock, Users, Calendar } from 'lucide-react';

interface RecipeDetails {
    servingSize: number;
    recipeIngredients: RecipeIngredient[];
    recipeSteps: RecipeStep[];
}

interface RecipeDetailViewProps {
    recipe: Recipe;
    recipeDetails: RecipeDetails | null;
    isLoadingDetails: boolean;
    originalRecipe?: Recipe | null;
}

// Utility function to format tag names from ALL_CAPS_WITH_UNDERSCORES to Capitalized
function formatTagName(tagName: string): string {
    return tagName
        .toLowerCase()
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

export function RecipeDetailView({
    recipe,
    recipeDetails,
    isLoadingDetails,
    originalRecipe,
}: RecipeDetailViewProps) {
    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });
    };

    return (
        <div className="space-y-8">
            {/* Recipe Header */}
            <div className="space-y-4">
                <div className="flex items-center gap-4 text-sm text-gray-600">
                    <div className="flex items-center gap-1">
                        <Calendar className="size-4" />
                        <span>Created {formatDate(recipe.createdAt)}</span>
                    </div>
                    {recipe.updatedAt !== recipe.createdAt && (
                        <div className="flex items-center gap-1">
                            <span>• Updated {formatDate(recipe.updatedAt)}</span>
                        </div>
                    )}
                </div>

                <h1 className="text-4xl font-bold text-gray-900">{recipe.title}</h1>

                <p className="text-lg text-gray-700 leading-relaxed">{recipe.description}</p>

                <div className="flex items-center gap-6 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                        <Users className="size-4" />
                        <span>{recipeDetails?.servingSize || recipe.servingSize} servings</span>
                    </div>
                    {recipe.forkedFrom && originalRecipe && (
                        <div className="flex items-center gap-2">
                            <svg className="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                            </svg>
                            <span className="text-gray-600 hover:text-gray-800 cursor-pointer italic underline" 
                                  onClick={() => window.location.href = `/recipes/${originalRecipe.id}`}>
                                Copied from {originalRecipe.title}
                            </span>
                        </div>
                    )}
                </div>
            </div>

            {/* Thumbnail */}
            {recipe.thumbnail && (
                <div className="space-y-2">
                    <Label>Recipe Image</Label>
                    <div className="relative rounded-lg overflow-hidden">
                        <Image
                            src={`data:image/jpeg;base64,${recipe.thumbnail.base64String}`}
                            alt={recipe.title}
                            width={800}
                            height={400}
                            className="w-full h-64 object-cover"
                        />
                    </div>
                </div>
            )}

            {/* Tags */}
            {recipe.tags && recipe.tags.length > 0 && (
                <div className="space-y-2">
                    <Label>Tags</Label>
                    <div className="flex flex-wrap gap-2">
                        {recipe.tags.map((tag) => (
                            <Badge
                                key={tag.id}
                                variant="secondary"
                                className="bg-[#FF7C75] text-white hover:bg-[#FF7C75]/90"
                            >
                                {formatTagName(tag.name)}
                            </Badge>
                        ))}
                    </div>
                </div>
            )}

            {/* Divider */}
            <div className="relative py-8">
                <div className="absolute inset-0 flex items-center" aria-hidden="true">
                    <div className="w-full border-t border-gray-200"></div>
                </div>
                <div className="relative flex justify-center">
                    <span className="bg-white px-6 text-sm font-medium text-gray-500">
                        Let&apos;s Get Cooking!
                    </span>
                </div>
            </div>

            {/* Ingredients */}
            <div className="space-y-4">
                <Label className="text-lg font-semibold">Ingredients</Label>
                {isLoadingDetails ? (
                    <div className="space-y-3">
                        {[...Array(4)].map((_, index) => (
                            <div key={index} className="animate-pulse">
                                <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                            </div>
                        ))}
                    </div>
                ) : recipeDetails?.recipeIngredients ? (
                    <div className="space-y-3">
                        {recipeDetails.recipeIngredients.map((ingredient, index) => (
                            <div
                                key={index}
                                className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg"
                            >
                                <div className="flex-1">
                                    <span className="font-medium text-gray-900">{ingredient.name}</span>
                                </div>
                                <div className="text-sm text-gray-600">
                                    <span className="font-medium">{ingredient.amount}</span>
                                    <span className="ml-1">{ingredient.unit}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="text-gray-500 italic">No ingredients available</p>
                )}
            </div>

            {/* Divider */}
            <div className="relative py-8">
                <div className="absolute inset-0 flex items-center" aria-hidden="true">
                    <div className="w-full border-t border-gray-200"></div>
                </div>
                <div className="relative flex justify-center">
                    <span className="bg-white px-6 text-sm font-medium text-gray-500">
                        Time to Work Your Magic!
                    </span>
                </div>
            </div>

            {/* Steps */}
            <div className="space-y-4">
                <Label className="text-lg font-semibold">Instructions</Label>
                {isLoadingDetails ? (
                    <div className="space-y-4">
                        {[...Array(4)].map((_, index) => (
                            <div key={index} className="animate-pulse">
                                <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                                <div className="h-4 bg-gray-200 rounded w-full"></div>
                            </div>
                        ))}
                    </div>
                ) : recipeDetails?.recipeSteps ? (
                    <div className="space-y-6">
                        {recipeDetails.recipeSteps.map((step, index) => (
                            <div key={index} className="flex gap-4">
                                <div className="flex-shrink-0">
                                    <div className="w-8 h-8 bg-[#FF7C75] text-white rounded-full flex items-center justify-center text-sm font-semibold">
                                        {step.order}
                                    </div>
                                </div>
                                <div className="flex-1">
                                    <p className="text-gray-900 leading-relaxed">{step.details}</p>
                                    {step.recipeImageDTOS && step.recipeImageDTOS.length > 0 && (
                                        <div className="mt-3">
                                            <Image
                                                src={`data:image/jpeg;base64,${step.recipeImageDTOS[0].base64String}`}
                                                alt={`Step ${step.order}`}
                                                width={400}
                                                height={300}
                                                className="rounded-lg object-cover max-w-xs"
                                            />
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="text-gray-500 italic">No instructions available</p>
                )}
            </div>
        </div>
    );
} 