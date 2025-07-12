import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getBranchHistory, getRecipe, getCommitChanges } from '@/lib/services/recipeService';
import { Recipe, CommitDTO, BranchDTO, ChangeResponse } from '@/lib/types/recipe';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ArrowLeft, GitCommit, Calendar, GitBranch, BarChart3 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { UserInfo } from '@/components/user';

export default function RecipeHistoryPage() {
    const params = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();
    const [recipe, setRecipe] = useState<Recipe | null>(null);
    const [commits, setCommits] = useState<CommitDTO[]>([]);
    const [commitChanges, setCommitChanges] = useState<Record<number, ChangeResponse>>({});
    const [isLoading, setIsLoading] = useState(true);
    const [currentBranch, setCurrentBranch] = useState<BranchDTO | null>(null);

    const recipeId = params?.id ? Number(params.id) : null;
    const branchId = searchParams.get('branchId') ? Number(searchParams.get('branchId')) : null;
    const branchName = searchParams.get('branchName') || 'Unknown';

    useEffect(() => {
        if (!recipeId || !branchId || isNaN(recipeId) || isNaN(branchId)) {
            toast.error('Invalid recipe or branch ID');
            navigate('/recipes');
            return;
        }

        fetchData();
    }, [recipeId, branchId, navigate]);

    const fetchData = async () => {
        if (!recipeId || !branchId) return;

        try {
            setIsLoading(true);

            // Fetch recipe and branch history in parallel
            const [recipeData, historyData] = await Promise.all([
                getRecipe(recipeId),
                getBranchHistory(branchId)
            ]);

            setRecipe(recipeData);
            setCommits(historyData);

            // Set current branch info
            setCurrentBranch({
                id: branchId,
                name: branchName,
                recipeId: recipeId,
                headCommitId: historyData[0]?.id || 0,
                createdAt: historyData[0]?.createdAt || new Date().toISOString()
            });

            // Fetch change details for each commit
            const changesPromises = historyData.map(commit =>
                getCommitChanges(commit.id).catch(error => {
                    console.error(`Failed to fetch changes for commit ${commit.id}:`, error);
                    return null;
                })
            );

            const changesResults = await Promise.all(changesPromises);
            const changesMap: Record<number, ChangeResponse> = {};

            changesResults.forEach((changes, index) => {
                if (changes) {
                    changesMap[historyData[index].id] = changes;
                }
            });

            setCommitChanges(changesMap);
        } catch (error) {
            console.error('Failed to fetch history data:', error);
            toast.error('Failed to load recipe history');
        } finally {
            setIsLoading(false);
        }
    };

    const handleBackToRecipe = () => {
        if (branchId && branchName) {
            navigate(`/recipes/${recipeId}?branchId=${branchId}&branchName=${branchName}`);
        } else {
            navigate(`/recipes/${recipeId}`);
        }
    };

    const handleCommitClick = (commit: CommitDTO) => {
        navigate(`/recipes/${recipeId}/changes/${commit.id}?branchId=${branchId}&branchName=${branchName}`);
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now.getTime() - date.getTime();
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
        const diffDays = Math.floor(diffHours / 24);

        if (diffHours < 1) {
            return 'Just now';
        } else if (diffHours < 24) {
            return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        } else if (diffDays < 7) {
            return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        } else {
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
            });
        }
    };

    const formatCommitId = (id: number) => {
        return `#${id.toString().padStart(3, '0')}`;
    };

    const calculateChanges = (changes: ChangeResponse) => {
        let additions = 0;
        let deletions = 0;
        let modifications = 0;

        if (changes.firstCommit) {
            // First commit - everything is an addition
            additions += changes.currentDetails.recipeIngredients?.length || 0;
            additions += changes.currentDetails.recipeSteps?.length || 0;
            additions += 1; // serving size
        } else {
            const oldIngredients = changes.oldDetails.recipeIngredients || [];
            const newIngredients = changes.currentDetails.recipeIngredients || [];
            const oldSteps = changes.oldDetails.recipeSteps || [];
            const newSteps = changes.currentDetails.recipeSteps || [];

            // Count ingredient changes
            const addedIngredients = newIngredients.filter(newIng =>
                !oldIngredients.find(oldIng =>
                    oldIng.name === newIng.name && oldIng.unit === newIng.unit && oldIng.amount === newIng.amount
                )
            );
            const removedIngredients = oldIngredients.filter(oldIng =>
                !newIngredients.find(newIng =>
                    newIng.name === oldIng.name && newIng.unit === oldIng.unit && newIng.amount === oldIng.amount
                )
            );

            // Count step changes
            const addedSteps = newSteps.filter(newStep =>
                !oldSteps.find(oldStep =>
                    oldStep.order === newStep.order && oldStep.details === newStep.details
                )
            );
            const removedSteps = oldSteps.filter(oldStep =>
                !newSteps.find(newStep =>
                    newStep.order === oldStep.order && newStep.details === oldStep.details
                )
            );

            // Count serving size changes
            const servingSizeChanged = changes.oldDetails.servingSize !== changes.currentDetails.servingSize;

            additions = addedIngredients.length + addedSteps.length;
            deletions = removedIngredients.length + removedSteps.length;
            modifications = servingSizeChanged ? 1 : 0;
        }

        return { additions, deletions, modifications };
    };

    if (isLoading) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-4xl'>
                    <div className='animate-pulse'>
                        <div className='mb-6 h-8 w-1/3 rounded bg-gray-200'></div>
                        <div className='space-y-4'>
                            {[...Array(5)].map((_, index) => (
                                <div key={index} className='h-20 rounded-lg bg-gray-200'></div>
                            ))}
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
                    <Button onClick={handleBackToRecipe} className='bg-[#FF7C75] hover:bg-[#FF7C75]/90'>
                        Back to Recipes
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className='container mx-auto px-4 py-8'>
            <div className='mx-auto max-w-4xl'>
                {/* Header */}
                <div className='mb-8'>
                    <Button
                        onClick={handleBackToRecipe}
                        variant='outline'
                        size='sm'
                        className='mb-4 border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white'
                    >
                        <ArrowLeft className='mr-2 size-4' />
                        Back to Recipe
                    </Button>

                    <div className='space-y-2'>
                        <h1 className='text-3xl font-bold text-gray-900'>{recipe.title}</h1>
                        <div className='flex items-center gap-4 text-sm text-gray-600'>
                            <div className='flex items-center gap-2'>
                                <GitBranch className='size-4' />
                                <span>Branch: <span className='font-medium text-[#FF7C75]'>{branchName}</span></span>
                            </div>
                            <div className='flex items-center gap-2'>
                                <GitCommit className='size-4' />
                                <span>{commits.length} commit{commits.length !== 1 ? 's' : ''}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Commit History */}
                <div className='space-y-4'>
                    <h2 className='text-xl font-semibold text-gray-900'>Commit History</h2>

                    {commits.length === 0 ? (
                        <div className='rounded-lg border border-gray-200 p-8 text-center'>
                            <GitCommit className='mx-auto mb-4 size-12 text-gray-400' />
                            <h3 className='mb-2 text-lg font-medium text-gray-900'>No commits yet</h3>
                            <p className='text-gray-600'>This branch doesn&apos;t have any commits yet.</p>
                        </div>
                    ) : (
                        <ScrollArea className='h-[600px]'>
                            <div className='space-y-3 pr-4'>
                                {commits.map((commit, index) => (
                                    <div key={commit.id} className='flex items-start gap-4'>
                                        {/* Commit indicator - moved outside */}
                                        <div className='relative flex flex-col items-center pt-4'>
                                            <div className='flex size-8 items-center justify-center rounded-full bg-[#FF7C75] text-white'>
                                                <GitCommit className='size-4' />
                                            </div>
                                            {index < commits.length - 1 && (
                                                <div className='mt-2 h-12 w-px bg-gray-200'></div>
                                            )}
                                        </div>

                                        {/* Commit card */}
                                        <div
                                            onClick={() => handleCommitClick(commit)}
                                            className='group cursor-pointer rounded-lg border border-gray-200 p-4 transition-all hover:border-[#FF7C75] hover:shadow-md flex-1'
                                        >
                                            <div className='flex items-start justify-between gap-4'>
                                                <div className='flex-1 min-w-0'>
                                                    <h3 className='text-sm font-medium text-gray-900 group-hover:text-[#FF7C75] transition-colors'>
                                                        {commit.message}
                                                    </h3>
                                                    <div className='mt-1 flex items-center gap-4 text-xs text-gray-500'>
                                                        {commitChanges[commit.id] && (
                                                            <div className='flex items-center gap-1'>
                                                                <BarChart3 className='size-3' />
                                                                <span>
                                                                    {(() => {
                                                                        const changeCounts = calculateChanges(commitChanges[commit.id]);
                                                                        const parts = [];
                                                                        if (changeCounts.additions > 0) {
                                                                            parts.push(`${changeCounts.additions} addition${changeCounts.additions > 1 ? 's' : ''}`);
                                                                        }
                                                                        if (changeCounts.deletions > 0) {
                                                                            parts.push(`${changeCounts.deletions} deletion${changeCounts.deletions > 1 ? 's' : ''}`);
                                                                        }
                                                                        if (changeCounts.modifications > 0) {
                                                                            parts.push(`${changeCounts.modifications} modification${changeCounts.modifications > 1 ? 's' : ''}`);
                                                                        }
                                                                        return parts.join(', ') || 'No changes';
                                                                    })()}
                                                                </span>
                                                            </div>
                                                        )}
                                                        <div className='flex items-center gap-1'>
                                                            <Calendar className='size-3' />
                                                            <span>{formatDate(commit.createdAt)}</span>
                                                        </div>
                                                        <span className='font-mono text-[#FF7C75]'>{formatCommitId(commit.id)}</span>
                                                    </div>

                                                    {/* User info */}
                                                    <div className='mt-2 flex items-center gap-2'>
                                                        <span className='text-xs text-gray-500'>By:</span>
                                                        <UserInfo
                                                            userId={commit.userId}
                                                            size='sm'
                                                            clickable={true}
                                                        />
                                                    </div>
                                                </div>

                                                {/* Commit metadata */}
                                                <div className='flex flex-col items-end text-xs text-gray-500 gap-1'>
                                                    {index === 0 && (
                                                        <span className='rounded-full bg-green-100 px-2 py-1 text-green-700 font-medium'>
                                                            Latest
                                                        </span>
                                                    )}
                                                    {commit.parentId === null && (
                                                        <span className='rounded-full bg-blue-100 px-2 py-1 text-blue-700 font-medium'>
                                                            Initial
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </ScrollArea>
                    )}
                </div>
            </div>
        </div>
    );
} 