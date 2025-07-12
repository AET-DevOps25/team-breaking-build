import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { RecipeForm } from '@/components/recipe/recipe-form';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import {
    getRecipe,
    getRecipeDetails,
    getRecipeBranches,
    updateRecipe,
    commitToBranch,
    getTags,
    Tag,
} from '@/lib/services/recipeService';
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
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [availableTags, setAvailableTags] = useState<Tag[]>([]);
    const [isLoadingTags, setIsLoadingTags] = useState(true);
    const [selectedTags, setSelectedTags] = useState<Tag[]>([]);
    const [prefillData, setPrefillData] = useState<RecipeFormData | undefined>(undefined);
    const [recipe, setRecipe] = useState<{
        id: number;
        title: string;
        description: string;
        servingSize: number;
        tags?: Array<{ id: number; name: string }>;
        thumbnail?: { base64String?: string };
        userId?: string;
        createdAt: string;
        updatedAt: string;
        forkedFrom?: number;
    } | null>(null);
    const [recipeDetails, setRecipeDetails] = useState<{
        servingSize: number;
        recipeIngredients: Array<{
            name: string;
            unit: string;
            amount: number;
        }>;
        recipeSteps: Array<{
            order: number;
            details: string;
            image?: string;
            images?: Array<{ base64String?: string }>;
        }>;
    } | null>(null);
    const [selectedBranch, setSelectedBranch] = useState<{
        id: number;
        name: string;
        recipeId: number;
        headCommitId: number;
        createdAt: string;
    } | null>(null);
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
            navigate('/recipes');
            return;
        }

        const fetchData = async () => {
            try {
                // Fetch recipe metadata
                const recipeData = await getRecipe(recipeId);
                if (!recipeData) {
                    toast.error('Recipe not found');
                    navigate('/recipes');
                    return;
                }

                // TypeScript assertion: we know recipeData is not null after the check above
                const recipe = recipeData as NonNullable<typeof recipeData>;
                setRecipe(recipe);

                // Fetch recipe details
                const details = await getRecipeDetails(recipeId);
                setRecipeDetails(details);

                // Fetch branches and determine which branch to use
                const branches = await getRecipeBranches(recipeId);
                
                // Check if branch information is provided in URL parameters
                const urlBranchId = searchParams.get('branchId');
                const urlBranchName = searchParams.get('branchName');
                
                let targetBranch = null;
                
                if (urlBranchId && urlBranchName) {
                    // Try to find the branch by ID
                    targetBranch = branches.find((branch) => branch.id === Number(urlBranchId));
                    
                    // If not found by ID, try by name as fallback
                    if (!targetBranch) {
                        targetBranch = branches.find((branch) => branch.name === urlBranchName);
                    }
                }
                
                // If no branch found from URL params, default to main branch
                if (!targetBranch) {
                    targetBranch = branches.find((branch) => branch.name === 'main');
                }
                
                setSelectedBranch(targetBranch || null);

                // Prepare prefill data
                const prefill: RecipeFormData = {
                    title: recipe.title,
                    description: recipe.description,
                    servingSize: recipe.servingSize,
                    tags: recipe.tags?.map((tag: { id: number; name: string }) => tag.name) || [],
                    ingredients:
                        details?.recipeIngredients?.map((ing: { name: string; amount: number; unit: string }) => ({
                            name: ing.name,
                            amount: ing.amount,
                            unit: ing.unit,
                        })) || [],
                    steps:
                        details?.recipeSteps?.map((step: { order: number; details: string }) => ({
                            order: step.order,
                            details: step.details,
                            // Note: We can't prefill images as File objects, they need to be uploaded again
                        })) || [],
                };

                // Prepare existing images for display
                const existingImages = {
                    thumbnail: recipe.thumbnail?.base64String,
                    stepImages:
                        details?.recipeSteps?.map(
                            (step: { order: number; details: string; images?: Array<{ base64String?: string }> }) =>
                                step.images?.[0]?.base64String || null,
                        ) || [],
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
                navigate('/recipes');
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
    }, [recipeId, navigate]);

    const handleSubmit = async (data: RecipeFormData) => {
        if (!user || !recipe || !selectedBranch || !recipeId) {
            toast.error('You must be logged in to edit a recipe');
            return;
        }

        setIsSubmitting(true);
        try {
            // Check for thumbnail changes - if a new thumbnail is uploaded, it's considered a metadata change
            const hasThumbnailChange = data.thumbnail !== originalData?.thumbnail;

            // Check if there are changes
            const hasMetadataChanges =
                data.title !== originalData?.title ||
                data.description !== originalData?.description ||
                data.servingSize !== originalData?.servingSize ||
                JSON.stringify(data.tags) !== JSON.stringify(originalData?.tags) ||
                hasThumbnailChange;

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

                await updateRecipe(recipeId!, metadata);
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

                await commitToBranch(selectedBranch.id, commitMessage, recipeDetails);
            }

            toast.success('Recipe updated successfully');
            // Navigate back to the detail page with branch information
            if (selectedBranch) {
                navigate(`/recipes/${recipeId}?branchId=${selectedBranch.id}&branchName=${selectedBranch.name}`);
            } else {
                navigate(`/recipes/${recipeId}`);
            }
        } catch (error) {
            console.error('Failed to update recipe:', error);
            toast.error('Failed to update recipe');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleFormChange = (data: RecipeFormData) => {
        if (!originalData) return;

        // Check for thumbnail changes - if a new thumbnail is uploaded, it's considered a metadata change
        const hasThumbnailChange = data.thumbnail !== originalData.thumbnail;

        const hasMetadataChanges =
            data.title !== originalData.title ||
            data.description !== originalData.description ||
            data.servingSize !== originalData.servingSize ||
            JSON.stringify(data.tags) !== JSON.stringify(originalData.tags) ||
            hasThumbnailChange;

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
                    <div className='animate-pulse'>
                        <div className='mb-4 h-8 w-1/3 rounded bg-gray-200'></div>
                        <div className='mb-8 h-4 w-1/2 rounded bg-gray-200'></div>
                        <div className='mb-8 h-64 rounded bg-gray-200'></div>
                        <div className='space-y-4'>
                            <div className='h-4 w-3/4 rounded bg-gray-200'></div>
                            <div className='h-4 w-1/2 rounded bg-gray-200'></div>
                            <div className='h-4 w-5/6 rounded bg-gray-200'></div>
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