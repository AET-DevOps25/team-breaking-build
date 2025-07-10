'use client';

import { useParams, useRouter } from 'next/navigation';
import { RecipeForm } from '@/components/recipe/recipe-form';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getRecipe, getRecipeDetails, getRecipeBranches, updateRecipe, commitToBranch, getTags, Tag } from '@/lib/services/recipeService';
import { fileToBase64 } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';

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

export default function EditRecipePage() {
    const params = useParams();
    const router = useRouter();
    const { user } = useAuth();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [availableTags, setAvailableTags] = useState<Tag[]>([]);
    const [isLoadingTags, setIsLoadingTags] = useState(true);
    const [selectedTags, setSelectedTags] = useState<Tag[]>([]);
    const [prefillData, setPrefillData] = useState<RecipeFormData | undefined>(undefined);
    const [recipe, setRecipe] = useState<any>(null);
    const [recipeDetails, setRecipeDetails] = useState<any>(null);
    const [mainBranch, setMainBranch] = useState<any>(null);
    const [commitMessage, setCommitMessage] = useState('');
      const [hasChanges, setHasChanges] = useState(false);
  const [hasDetailsChanges, setHasDetailsChanges] = useState(false);
  const [originalData, setOriginalData] = useState<RecipeFormData | undefined>(undefined);
  const [existingImages, setExistingImages] = useState<{
    thumbnail?: string;
    stepImages?: (string | null)[];
  }>({});

    const recipeId = params?.id ? Number(params.id) : null;

    useEffect(() => {
        if (!recipeId) {
            toast.error('Invalid recipe ID');
            router.push('/recipes');
            return;
        }

        const fetchData = async () => {
            try {
                // Fetch recipe metadata
                const recipeData = await getRecipe(recipeId);
                setRecipe(recipeData);

                // Fetch recipe details
                const details = await getRecipeDetails(recipeId);
                setRecipeDetails(details);

                // Fetch branches to get main branch
                const branches = await getRecipeBranches(recipeId);
                const main = branches.find((branch: any) => branch.name === 'main');
                setMainBranch(main);

                // Prepare prefill data
                const prefill: RecipeFormData = {
                    title: recipeData.title,
                    description: recipeData.description,
                    servingSize: recipeData.servingSize,
                    tags: recipeData.tags?.map((tag: any) => tag.name) || [],
                    ingredients: details?.recipeIngredients?.map((ing: any) => ({
                        name: ing.name,
                        amount: ing.amount,
                        unit: ing.unit,
                    })) || [],
                    steps: details?.recipeSteps?.map((step: any) => ({
                        order: step.order,
                        details: step.details,
                        // Note: We can't prefill images as File objects, they need to be uploaded again
                    })) || [],
                };

                // Prepare existing images for display
                const existingImages = {
                    thumbnail: recipeData.thumbnail,
                    stepImages: details?.recipeSteps?.map((step: any) => step.image) || [],
                };

                setPrefillData(prefill);
                setOriginalData(prefill);
                setExistingImages(existingImages);

                // Set selected tags from recipe data
                if (recipeData.tags) {
                    setSelectedTags(recipeData.tags);
                }
            } catch (error) {
                console.error('Failed to fetch recipe data:', error);
                toast.error('Failed to load recipe data');
                router.push('/recipes');
            }
        };

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

        fetchData();
        fetchTags();
    }, [recipeId, router]);

    const handleSubmit = async (data: RecipeFormData) => {
        if (!user || !recipe || !mainBranch) {
            toast.error('You must be logged in to edit a recipe');
            return;
        }

        setIsSubmitting(true);
        try {
            // Check if there are changes
            const hasMetadataChanges =
                data.title !== originalData?.title ||
                data.description !== originalData?.description ||
                data.servingSize !== originalData?.servingSize ||
                JSON.stringify(data.tags) !== JSON.stringify(originalData?.tags);

            const hasDetailsChanges =
                JSON.stringify(data.ingredients) !== JSON.stringify(originalData?.ingredients) ||
                JSON.stringify(data.steps) !== JSON.stringify(originalData?.steps);

            if (!hasMetadataChanges && !hasDetailsChanges) {
                toast.error('No changes detected. Please make changes before updating.');
                return;
            }

            // Update metadata if there are metadata changes
            if (hasMetadataChanges) {
                // Convert thumbnail file to RecipeImage format if present
                let thumbnail: { base64String: string } | undefined = undefined;
                if (data.thumbnail) {
                    const base64String = await fileToBase64(data.thumbnail);
                    thumbnail = { base64String };
                }

                const metadata = {
                    title: data.title,
                    description: data.description,
                    servingSize: data.servingSize,
                    tags: selectedTags.map((tag: Tag) => ({ id: tag.id, name: tag.name })),
                    ...(thumbnail ? { thumbnail } : {}),
                };

                await updateRecipe(recipeId, metadata);
            }

            // Update recipe details if there are details changes
            if (hasDetailsChanges) {
                if (!commitMessage.trim()) {
                    toast.error('Commit message is required when updating recipe details');
                    return;
                }

                // Prepare recipeSteps with async base64 conversion for step images
                const recipeSteps = await Promise.all(
                    data.steps.map(async (step) => ({
                        order: step.order,
                        details: step.details,
                        ...(step.image ? { recipeImageDTOS: [{ base64String: await fileToBase64(step.image) }] } : {}),
                    })),
                );

                const recipeDetails = {
                    servingSize: data.servingSize,
                    images: [], // Optional, can be omitted or empty
                    recipeIngredients: data.ingredients.map((ing) => ({
                        name: ing.name,
                        unit: ing.unit,
                        amount: ing.amount,
                    })),
                    recipeSteps,
                };

                await commitToBranch(mainBranch.id, commitMessage, recipeDetails);
            }

            toast.success('Recipe updated successfully');
            router.push(`/recipes/${recipeId}`);
        } catch (error) {
            console.error('Failed to update recipe:', error);
            toast.error('Failed to update recipe');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleFormChange = (data: RecipeFormData) => {
        if (!originalData) return;

        const hasMetadataChanges =
            data.title !== originalData.title ||
            data.description !== originalData.description ||
            data.servingSize !== originalData.servingSize ||
            JSON.stringify(data.tags) !== JSON.stringify(originalData.tags);

        const detailsChanges =
            JSON.stringify(data.ingredients) !== JSON.stringify(originalData.ingredients) ||
            JSON.stringify(data.steps) !== JSON.stringify(originalData.steps);

        setHasChanges(hasMetadataChanges || detailsChanges);
        setHasDetailsChanges(detailsChanges);
    };

    if (!recipe || !recipeDetails) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-2xl'>
                    <div className="animate-pulse">
                        <div className="h-8 bg-gray-200 rounded w-1/3 mb-4"></div>
                        <div className="h-4 bg-gray-200 rounded w-1/2 mb-8"></div>
                        <div className="h-64 bg-gray-200 rounded mb-8"></div>
                        <div className="space-y-4">
                            <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                            <div className="h-4 bg-gray-200 rounded w-5/6"></div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className='container mx-auto px-4 py-8'>
            <div className='mx-auto max-w-2xl'>
                <h1 className='mb-8 text-3xl font-bold'>Edit Recipe</h1>

                        <RecipeForm
          onSubmit={handleSubmit}
          isSubmitting={isSubmitting}
          availableTags={availableTags}
          isLoadingTags={isLoadingTags}
          selectedTags={selectedTags}
          setSelectedTags={setSelectedTags}
          prefillData={prefillData}
          onChange={handleFormChange}
          submitButtonText={isSubmitting ? 'Updating...' : 'Update Recipe'}
          disabled={!hasChanges || (hasDetailsChanges && !commitMessage.trim())}
          commitMessage={commitMessage}
          onCommitMessageChange={setCommitMessage}
          showCommitMessage={hasDetailsChanges}
          existingImages={existingImages}
        />
            </div>
        </div>
    );
} 