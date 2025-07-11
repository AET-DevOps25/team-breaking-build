'use client';

import { useRouter } from 'next/navigation';
import { RecipeForm } from '@/components/recipe/recipe-form';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { createRecipe, getTags, Tag } from '@/lib/services/recipeService';
import { fileToBase64 } from '@/lib/utils';

// Type for the form data
type RecipeFormData = {
  title: string;
  description: string;
  servingSize: number;
  tags: string[];
  thumbnail?: File;
  ingredients: Array<{
    name: string;
    amount: number;
    unit: string;
  }>;
  steps: Array<{
    order: number;
    details: string;
    image?: File;
  }>;
};

// Types for prefill data parsing
type PrefillIngredient = {
  name?: string;
  amount?: number;
  unit?: string;
};

type PrefillStep = {
  order?: number;
  details?: string;
};

type PrefillDataRaw = {
  title?: string;
  description?: string;
  servingSize?: number;
  tags?: string[];
  recipeIngredients?: PrefillIngredient[];
  ingredients?: PrefillIngredient[];
  recipeSteps?: PrefillStep[];
  steps?: PrefillStep[];
};

export default function CreateRecipePage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [availableTags, setAvailableTags] = useState<Tag[]>([]);
  const [isLoadingTags, setIsLoadingTags] = useState(true);
  const [selectedTags, setSelectedTags] = useState<Tag[]>([]);

  // Get prefill data from sessionStorage during component initialization
  const prefillData: RecipeFormData | undefined = (() => {
    if (typeof window !== 'undefined') {
      const storedData = sessionStorage.getItem('recipePrefillData');
      if (storedData) {
        try {
          const parsedData: PrefillDataRaw = JSON.parse(storedData);

          // Transform the data to match RecipeFormData interface
          const transformedData: RecipeFormData = {
            title: parsedData.title || '',
            description: parsedData.description || '',
            servingSize: parsedData.servingSize || 4,
            tags: parsedData.tags || [],
            // Transform recipeIngredients to ingredients
            ingredients: (parsedData.recipeIngredients || parsedData.ingredients || []).map(
              (ing: PrefillIngredient) => ({
                name: ing.name || '',
                amount: ing.amount || 0,
                unit: ing.unit || '',
              }),
            ),
            // Transform recipeSteps to steps
            steps: (parsedData.recipeSteps || parsedData.steps || []).map((step: PrefillStep, index: number) => ({
              order: step.order || index + 1,
              details: step.details || '',
            })),
          };

          console.log('Prefill data loaded from sessionStorage:', transformedData);
          return transformedData;
        } catch (error) {
          console.error('Error parsing prefill data:', error);
          sessionStorage.removeItem('recipePrefillData');
        }
      }
    }
    return undefined;
  })();

  useEffect(() => {
    const fetchTags = async () => {
      try {
        const tags = await getTags();
        setAvailableTags(tags);
      } catch {
        toast.error('Failed to load available tags');
      } finally {
        setIsLoadingTags(false);
      }
    };

    fetchTags();
  }, []);

  // Clear sessionStorage after component has mounted and prefillData is set
  useEffect(() => {
    if (prefillData && typeof window !== 'undefined') {
      // Clear the sessionStorage after the prefill data has been successfully loaded
      sessionStorage.removeItem('recipePrefillData');
      console.log('Cleared sessionStorage after successful prefill');
    }
  }, [prefillData]);

  const handleSubmit = async (data: RecipeFormData) => {
    setIsSubmitting(true);
    try {
      // Convert thumbnail file to RecipeImage format if present
      let thumbnail: { base64String: string } | undefined = undefined;
      if (data.thumbnail) {
        const base64String = await fileToBase64(data.thumbnail);
        thumbnail = { base64String };
      }

      // Prepare recipeSteps with async base64 conversion for step images
      const recipeSteps = await Promise.all(
        data.steps.map(async (step) => ({
          order: step.order,
          details: step.details,
          ...(step.image ? { recipeImageDTOS: [{ base64String: await fileToBase64(step.image) }] } : {}),
        })),
      );

      // Map RecipeFormData to CreateRecipeRequest DTO
      const createRequest = {
        metadata: {
          title: data.title,
          description: data.description,
          servingSize: data.servingSize,
          ...(selectedTags.length > 0
            ? { tags: selectedTags.map((tag: Tag) => ({ id: tag.id, name: tag.name })) }
            : {}),
          ...(thumbnail ? { thumbnail } : {}),
        },
        initRequest: {
          recipeDetails: {
            servingSize: data.servingSize,
            images: [], // Optional, can be omitted or empty
            recipeIngredients: data.ingredients.map((ing) => ({
              name: ing.name,
              unit: ing.unit,
              amount: ing.amount,
            })),
            recipeSteps,
          },
        },
      };

      await createRecipe(createRequest);
      toast.success('Recipe created successfully');
      router.push('/recipes');
    } catch {
      toast.error('Failed to create recipe');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className='container mx-auto px-4 py-8'>
      <div className='mx-auto max-w-2xl'>
        <h1 className='mb-8 text-3xl font-bold'>Create New Recipe</h1>
        <RecipeForm
          onSubmit={handleSubmit}
          isSubmitting={isSubmitting}
          availableTags={availableTags}
          isLoadingTags={isLoadingTags}
          selectedTags={selectedTags}
          setSelectedTags={setSelectedTags}
          prefillData={prefillData}
        />
      </div>
    </div>
  );
}
