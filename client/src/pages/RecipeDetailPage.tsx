import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getRecipe, getRecipeDetails, deleteRecipe, copyRecipe, getRecipeBranches, getCommitDetails } from '@/lib/services/recipeService';
import { Recipe, RecipeIngredient, RecipeStep, BranchDTO } from '@/lib/types/recipe';
import { RecipeDetailView } from '@/components/recipe/recipe-detail-view';
import { RecipeActionButtons } from '@/components/recipe/recipe-action-buttons';
import { useAuth } from '@/contexts/AuthContext';

interface RecipeDetails {
    servingSize: number;
    recipeIngredients: RecipeIngredient[];
    recipeSteps: RecipeStep[];
}

export default function RecipeDetailPage() {
    const params = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();
    const [recipe, setRecipe] = useState<Recipe | null>(null);
    const [recipeDetails, setRecipeDetails] = useState<RecipeDetails | null>(null);
    const [originalRecipe, setOriginalRecipe] = useState<Recipe | null>(null);
    const [currentBranch, setCurrentBranch] = useState<BranchDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoadingDetails, setIsLoadingDetails] = useState(true);
    const [recipeId, setRecipeId] = useState<number | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isCloning, setIsCloning] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    // Extract recipe ID from params
    useEffect(() => {
        if (params?.id) {
            const recipeIdParam = params.id;
            const extractedId = Number(recipeIdParam);

            console.log('Params:', params);
            console.log('Recipe ID Param:', recipeIdParam);
            console.log('Recipe ID (extracted):', extractedId);

            setRecipeId(extractedId);
        }
    }, [params]);

    // Handle invalid recipe ID
    useEffect(() => {
        if (recipeId !== null && isNaN(recipeId)) {
            toast.error('Invalid recipe ID');
            navigate('/recipes');
        }
    }, [recipeId, navigate]);

    useEffect(() => {
        if (!recipeId || isNaN(recipeId)) {
            return; // Don't redirect immediately, wait for recipeId to be set
        }

        const fetchRecipe = async () => {
            try {
                setIsLoading(true);
                const recipeData = await getRecipe(recipeId);
                setRecipe(recipeData);

                // If this is a forked recipe, fetch the original recipe
                if (recipeData && recipeData.forkedFrom) {
                    await fetchOriginalRecipe(recipeData.forkedFrom);
                }

                // Initialize the current branch (default to main branch)
                await initializeBranch(recipeId);
            } catch (error) {
                console.error('Failed to fetch recipe:', error);
                toast.error('Failed to load recipe');
                navigate('/recipes');
            } finally {
                setIsLoading(false);
            }
        };

        const fetchOriginalRecipe = async (forkedFromId: number) => {
            try {
                const original = await getRecipe(forkedFromId);
                setOriginalRecipe(original);
            } catch (error) {
                console.error('Failed to fetch original recipe:', error);
                // Don't show error toast for this as it's not critical
            }
        };

        fetchRecipe();
    }, [recipeId, navigate]);

    // Initialize the current branch and fetch recipe details
    const initializeBranch = async (recipeId: number) => {
        try {
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

            if (targetBranch) {
                setCurrentBranch(targetBranch);
                await fetchRecipeDetailsForBranch(targetBranch.headCommitId);
            } else {
                console.warn('No suitable branch found for recipe:', recipeId);
            }
        } catch (error) {
            console.error('Failed to initialize branch:', error);
            toast.error('Failed to load recipe branches');
        }
    };

    // Fetch recipe details for a specific commit
    const fetchRecipeDetailsForBranch = async (commitId: number) => {
        try {
            setIsLoadingDetails(true);
            const response = await getCommitDetails(commitId);

            if (response?.recipeDetails) {
                setRecipeDetails(response.recipeDetails);
            }
        } catch (error) {
            console.error('Failed to fetch recipe details:', error);
            toast.error('Failed to load recipe details');
        } finally {
            setIsLoadingDetails(false);
        }
    };

    // Handle branch change
    const handleBranchChange = async (branch: BranchDTO) => {
        setCurrentBranch(branch);
        await fetchRecipeDetailsForBranch(branch.headCommitId);
    };

    // Handle history button click
    const handleHistoryClick = () => {
        if (currentBranch && recipeId) {
            navigate(`/recipes/${recipeId}/history?branchId=${currentBranch.id}&branchName=${currentBranch.name}`);
        }
    };

    // Fix ownership check - compare user.id with recipe.userId
    const isOwner = Boolean(user && recipe && user.id === recipe.userId);

    const handleCopy = async () => {
        if (!recipe || !user || !currentBranch) {
            toast.error('You must be logged in to clone a recipe');
            return;
        }

        try {
            setIsCloning(true);
            const clonedRecipe = await copyRecipe(recipe.id, currentBranch.id);

            if (clonedRecipe) {
                toast.success('Recipe successfully cloned!');
                // Navigate to the cloned recipe's detail page
                navigate(`/recipes/${clonedRecipe.id}`);
            } else {
                toast.error('Failed to clone recipe');
            }
        } catch (error) {
            console.error('Failed to clone recipe:', error);
            toast.error('Failed to clone recipe. Please try again.');
        } finally {
            setIsCloning(false);
        }
    };

    const handleEdit = () => {
        if (currentBranch) {
            navigate(`/recipes/${recipeId}/edit?branchId=${currentBranch.id}&branchName=${currentBranch.name}`);
        } else {
            navigate(`/recipes/${recipeId}/edit`);
        }
    };

    const handleDelete = async () => {
        if (!recipe || !user) {
            toast.error('You must be logged in to delete a recipe');
            return;
        }
        setShowDeleteDialog(true);
    };

    const confirmDelete = async () => {
        if (!recipe) {
            setShowDeleteDialog(false);
            return;
        }
        try {
            setIsDeleting(true);
            await deleteRecipe(recipe.id);
            toast.success('Recipe successfully deleted!');
            navigate('/recipes');
        } catch (error) {
            console.error('Failed to delete recipe:', error);
            toast.error('Failed to delete recipe. Please try again.');
        } finally {
            setIsDeleting(false);
            setShowDeleteDialog(false);
        }
    };

    if (isLoading || !recipeId || isNaN(recipeId)) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-4xl'>
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

    if (!recipe) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-4xl text-center'>
                    <h1 className='mb-4 text-2xl font-bold text-gray-900'>Recipe Not Found</h1>
                    <p className='mb-8 text-gray-600'>
                        The recipe you&apos;re looking for doesn&apos;t exist or has been removed.
                    </p>
                    <button
                        onClick={() => navigate('/recipes')}
                        className='rounded-lg bg-[#FF7C75] px-6 py-2 text-white transition-colors hover:bg-[#FF7C75]/90'
                    >
                        Back to Recipes
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className='container mx-auto px-4 py-8'>
            <div className='mx-auto max-w-4xl'>
                <RecipeActionButtons
                    recipeId={recipe.id}
                    currentBranch={currentBranch}
                    onBranchChange={handleBranchChange}
                    onHistoryClick={handleHistoryClick}
                    isOwner={isOwner}
                    onCopy={handleCopy}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                    isDeleting={isDeleting}
                    isCloning={isCloning}
                />

                <RecipeDetailView
                    recipe={recipe}
                    recipeDetails={recipeDetails}
                    isLoadingDetails={isLoadingDetails}
                    originalRecipe={originalRecipe}
                />
            </div>
            {/* Delete Confirmation Dialog */}
            {showDeleteDialog && (
                <div className='fixed inset-0 z-50 flex items-center justify-center'>
                    <div
                        className='fixed inset-0 bg-black/50 backdrop-blur-sm'
                        onClick={() => setShowDeleteDialog(false)}
                    />
                    <div className='relative bg-white rounded-lg p-6 w-full max-w-md mx-4 shadow-xl'>
                        <div className='flex items-center justify-between mb-4'>
                            <h3 className='text-lg font-semibold text-gray-900'>Delete Recipe</h3>
                        </div>
                        <p className='text-gray-600 mb-6'>
                            Are you sure you want to delete this recipe? This action cannot be undone.
                        </p>
                        <div className='flex justify-end gap-3'>
                            <button
                                onClick={() => setShowDeleteDialog(false)}
                                className='px-4 py-2 text-gray-600 hover:text-gray-800 transition-colors'
                                disabled={isDeleting}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDelete}
                                disabled={isDeleting}
                                className='px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors disabled:opacity-50'
                            >
                                {isDeleting ? 'Deleting...' : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
} 