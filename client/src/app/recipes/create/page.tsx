'use client';

import { useRouter } from 'next/navigation';
import { RecipeForm } from '@/components/recipe/recipe-form';
import { useState } from 'react';
import { toast } from 'sonner';
import { createRecipe } from '@/lib/services/recipeService';
import { RecipeFormData } from '@/lib/types/recipe';

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
