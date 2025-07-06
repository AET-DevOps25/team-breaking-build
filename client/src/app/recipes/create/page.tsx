'use client';

import { useRouter } from 'next/navigation';
import { RecipeForm } from '@/components/recipe/recipe-form';
import { useState } from 'react';
import { toast } from 'sonner';
import { createRecipe } from '@/lib/services/recipeService';

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

export default function CreateRecipePage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (data: RecipeFormData) => {
    setIsSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('title', data.title);
      formData.append('description', data.description);
      formData.append('servingSize', data.servingSize.toString());
      data.tags.forEach((tag: string) => {
        formData.append('tags', tag);
      });
      if (data.thumbnail) {
        formData.append('thumbnail', data.thumbnail);
      }

      // Add ingredients and steps as JSON strings
      formData.append('ingredients', JSON.stringify(data.ingredients));
      formData.append('steps', JSON.stringify(data.steps));

      await createRecipe(formData);
      toast.success('Recipe created successfully');
      router.push('/recipes');
    } catch (error) {
      toast.error('Failed to create recipe');
      console.error('Error creating recipe:', error);
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
        />
      </div>
    </div>
  );
}
