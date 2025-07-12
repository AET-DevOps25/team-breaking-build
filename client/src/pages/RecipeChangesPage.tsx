import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getCommitChanges, getCommitDetails } from '@/lib/services/recipeService';
import { ChangeResponse, CommitDetailsResponse } from '@/lib/types/recipe';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { ArrowLeft, GitCommit, Calendar, Plus, Minus, FileText, ChefHat, BarChart3, Info } from 'lucide-react';
import { UserInfo } from '@/components/user';

export default function RecipeChangesPage() {
    const params = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [changes, setChanges] = useState<ChangeResponse | null>(null);
    const [commitDetails, setCommitDetails] = useState<CommitDetailsResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const recipeId = params?.id ? Number(params.id) : null;
    const commitId = params?.commitId ? Number(params.commitId) : null;
    const branchId = searchParams.get('branchId') ? Number(searchParams.get('branchId')) : null;
    const branchName = searchParams.get('branchName') || 'Unknown';

    useEffect(() => {
        if (!recipeId || !commitId || isNaN(recipeId) || isNaN(commitId)) {
            toast.error('Invalid recipe or commit ID');
            navigate('/recipes');
            return;
        }

        fetchData();
    }, [recipeId, commitId, navigate]);

    const fetchData = async () => {
        if (!commitId) return;

        try {
            setIsLoading(true);

            // Fetch commit changes and details in parallel
            const [changesData, detailsData] = await Promise.all([
                getCommitChanges(commitId),
                getCommitDetails(commitId)
            ]);

            setChanges(changesData);
            setCommitDetails(detailsData);
        } catch (error) {
            console.error('Failed to fetch commit changes:', error);
            toast.error('Failed to load commit changes');
        } finally {
            setIsLoading(false);
        }
    };

    const handleBackToHistory = () => {
        if (branchId) {
            navigate(`/recipes/${recipeId}/history?branchId=${branchId}&branchName=${branchName}`);
        } else {
            navigate(`/recipes/${recipeId}`);
        }
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const formatCommitId = (id: number) => {
        return `#${id.toString().padStart(3, '0')}`;
    };

    const calculateChanges = () => {
        if (!changes) return { additions: 0, deletions: 0, modifications: 0 };

        let additions = 0;
        let deletions = 0;
        let modifications = 0;

        // Count ingredient changes
        const oldIngredients = changes.oldDetails.recipeIngredients || [];
        const newIngredients = changes.currentDetails.recipeIngredients || [];

        // Count added ingredients
        newIngredients.forEach(newIng => {
            const exists = oldIngredients.find(oldIng =>
                oldIng.name === newIng.name && oldIng.unit === newIng.unit && oldIng.amount === newIng.amount
            );
            if (!exists) additions++;
        });

        // Count removed ingredients
        oldIngredients.forEach(oldIng => {
            const exists = newIngredients.find(newIng =>
                newIng.name === oldIng.name && newIng.unit === oldIng.unit && newIng.amount === oldIng.amount
            );
            if (!exists) deletions++;
        });

        // Count step changes
        const oldSteps = changes.oldDetails.recipeSteps || [];
        const newSteps = changes.currentDetails.recipeSteps || [];

        // Count added steps
        newSteps.forEach(newStep => {
            const exists = oldSteps.find(oldStep =>
                oldStep.order === newStep.order && oldStep.details === newStep.details
            );
            if (!exists) additions++;
        });

        // Count removed steps
        oldSteps.forEach(oldStep => {
            const exists = newSteps.find(newStep =>
                newStep.order === oldStep.order && newStep.details === oldStep.details
            );
            if (!exists) deletions++;
        });

        // Count serving size changes
        if (changes.oldDetails.servingSize !== changes.currentDetails.servingSize) {
            modifications++;
        }

        return { additions, deletions, modifications };
    };

    const renderIngredientChanges = () => {
        if (!changes) return null;

        const oldIngredients = changes.oldDetails.recipeIngredients || [];
        const newIngredients = changes.currentDetails.recipeIngredients || [];

        // Find completely new ingredients (by name)
        const addedIngredients = newIngredients.filter(newIng =>
            !oldIngredients.find(oldIng => oldIng.name.toLowerCase() === newIng.name.toLowerCase())
        );

        // Find completely removed ingredients (by name)
        const removedIngredients = oldIngredients.filter(oldIng =>
            !newIngredients.find(newIng => newIng.name.toLowerCase() === oldIng.name.toLowerCase())
        );

        // Find modified ingredients (same name, different amount/unit)
        const modifiedIngredients = newIngredients.filter(newIng => {
            const oldIng = oldIngredients.find(old => old.name.toLowerCase() === newIng.name.toLowerCase());
            return oldIng && (oldIng.amount !== newIng.amount || oldIng.unit !== newIng.unit);
        }).map(newIng => {
            const oldIng = oldIngredients.find(old => old.name.toLowerCase() === newIng.name.toLowerCase())!;
            return { old: oldIng, new: newIng };
        });

        // Check for reordering (same ingredients but different order)
        const hasReordering = JSON.stringify(oldIngredients.map(i => `${i.name}|${i.amount}|${i.unit}`))
            !== JSON.stringify(newIngredients.map(i => `${i.name}|${i.amount}|${i.unit}`)) &&
            addedIngredients.length === 0 && removedIngredients.length === 0 && modifiedIngredients.length === 0;

        if (addedIngredients.length === 0 && removedIngredients.length === 0 && modifiedIngredients.length === 0 && !hasReordering) {
            return null;
        }

        return (
            <div className='space-y-3'>
                <h3 className='text-lg font-semibold text-gray-900 flex items-center gap-2'>
                    <ChefHat className='size-5' />
                    Ingredients
                </h3>
                <div className='rounded-lg border border-gray-200 p-4 space-y-4'>
                    {addedIngredients.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-green-700'>
                                <Plus className='size-4' />
                                <span className='font-medium'>Added ({addedIngredients.length})</span>
                            </div>
                            <ul className='space-y-1 ml-6'>
                                {addedIngredients.map((ingredient, index) => (
                                    <li key={index} className='text-sm bg-green-50 text-green-800 px-3 py-2 rounded'>
                                        {ingredient.amount} {ingredient.unit} {ingredient.name}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {removedIngredients.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-red-700'>
                                <Minus className='size-4' />
                                <span className='font-medium'>Removed ({removedIngredients.length})</span>
                            </div>
                            <ul className='space-y-1 ml-6'>
                                {removedIngredients.map((ingredient, index) => (
                                    <li key={index} className='text-sm bg-red-50 text-red-800 px-3 py-2 rounded'>
                                        {ingredient.amount} {ingredient.unit} {ingredient.name}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {modifiedIngredients.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-blue-700'>
                                <FileText className='size-4' />
                                <span className='font-medium'>Modified ({modifiedIngredients.length})</span>
                            </div>
                            <ul className='space-y-2 ml-6'>
                                {modifiedIngredients.map((ingredient, index) => (
                                    <li key={index} className='text-sm space-y-1'>
                                        <div className='bg-blue-50 text-blue-800 px-3 py-2 rounded'>
                                            <div className='font-medium'>{ingredient.new.name}</div>
                                            <div className='flex items-center gap-4 text-xs mt-1'>
                                                <span className='bg-red-100 text-red-700 px-2 py-0.5 rounded'>
                                                    - {ingredient.old.amount} {ingredient.old.unit}
                                                </span>
                                                <span className='bg-green-100 text-green-700 px-2 py-0.5 rounded'>
                                                    + {ingredient.new.amount} {ingredient.new.unit}
                                                </span>
                                            </div>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {hasReordering && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-amber-700'>
                                <svg className='size-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'>
                                    <path strokeLinecap='round' strokeLinejoin='round' strokeWidth={2} d='M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4' />
                                </svg>
                                <span className='font-medium'>Reordered</span>
                            </div>
                            <div className='text-sm bg-amber-50 text-amber-800 px-3 py-2 rounded ml-6'>
                                Ingredients have been reordered. See the before/after comparison below for details.
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    const renderStepChanges = () => {
        if (!changes) return null;

        const oldSteps = changes.oldDetails.recipeSteps || [];
        const newSteps = changes.currentDetails.recipeSteps || [];

        // Find completely new steps (by content)
        const addedSteps = newSteps.filter(newStep =>
            !oldSteps.find(oldStep => oldStep.details.toLowerCase().trim() === newStep.details.toLowerCase().trim())
        );

        // Find completely removed steps (by content)
        const removedSteps = oldSteps.filter(oldStep =>
            !newSteps.find(newStep => newStep.details.toLowerCase().trim() === oldStep.details.toLowerCase().trim())
        );

        // Find modified steps (same content but different position)
        const repositionedSteps = newSteps.filter(newStep => {
            const oldStep = oldSteps.find(old => old.details.toLowerCase().trim() === newStep.details.toLowerCase().trim());
            return oldStep && oldStep.order !== newStep.order;
        }).map(newStep => {
            const oldStep = oldSteps.find(old => old.details.toLowerCase().trim() === newStep.details.toLowerCase().trim())!;
            return { old: oldStep, new: newStep };
        });

        // Check for content modifications (same order but different content)
        const modifiedSteps = newSteps.filter(newStep => {
            const oldStep = oldSteps.find(old => old.order === newStep.order);
            return oldStep && oldStep.details.toLowerCase().trim() !== newStep.details.toLowerCase().trim() &&
                !addedSteps.find(added => added.order === newStep.order) &&
                !removedSteps.find(removed => removed.order === oldStep.order);
        }).map(newStep => {
            const oldStep = oldSteps.find(old => old.order === newStep.order)!;
            return { old: oldStep, new: newStep };
        });

        // Check for pure reordering (same steps but different order)
        const hasReordering = JSON.stringify(oldSteps.map(s => s.details.toLowerCase().trim()).sort()) ===
            JSON.stringify(newSteps.map(s => s.details.toLowerCase().trim()).sort()) &&
            JSON.stringify(oldSteps.map(s => `${s.order}|${s.details}`)) !==
            JSON.stringify(newSteps.map(s => `${s.order}|${s.details}`)) &&
            addedSteps.length === 0 && removedSteps.length === 0 && modifiedSteps.length === 0;

        if (addedSteps.length === 0 && removedSteps.length === 0 && repositionedSteps.length === 0 && modifiedSteps.length === 0 && !hasReordering) {
            return null;
        }

        return (
            <div className='space-y-3'>
                <h3 className='text-lg font-semibold text-gray-900 flex items-center gap-2'>
                    <FileText className='size-5' />
                    Instructions
                </h3>
                <div className='rounded-lg border border-gray-200 p-4 space-y-4'>
                    {addedSteps.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-green-700'>
                                <Plus className='size-4' />
                                <span className='font-medium'>Added ({addedSteps.length})</span>
                            </div>
                            <ul className='space-y-2 ml-6'>
                                {addedSteps.map((step, index) => (
                                    <li key={index} className='text-sm bg-green-50 text-green-800 px-3 py-2 rounded'>
                                        <div className='flex items-start gap-2'>
                                            <span className='flex-shrink-0 size-5 bg-green-200 text-green-800 rounded-full flex items-center justify-center text-xs font-medium'>
                                                {step.order}
                                            </span>
                                            <span>{step.details}</span>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {removedSteps.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-red-700'>
                                <Minus className='size-4' />
                                <span className='font-medium'>Removed ({removedSteps.length})</span>
                            </div>
                            <ul className='space-y-2 ml-6'>
                                {removedSteps.map((step, index) => (
                                    <li key={index} className='text-sm bg-red-50 text-red-800 px-3 py-2 rounded'>
                                        <div className='flex items-start gap-2'>
                                            <span className='flex-shrink-0 size-5 bg-red-200 text-red-800 rounded-full flex items-center justify-center text-xs font-medium'>
                                                {step.order}
                                            </span>
                                            <span>{step.details}</span>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {modifiedSteps.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-blue-700'>
                                <FileText className='size-4' />
                                <span className='font-medium'>Modified ({modifiedSteps.length})</span>
                            </div>
                            <ul className='space-y-2 ml-6'>
                                {modifiedSteps.map((step, index) => (
                                    <li key={index} className='text-sm space-y-2'>
                                        <div className='bg-blue-50 text-blue-800 px-3 py-2 rounded'>
                                            <div className='font-medium mb-2 flex items-center gap-2'>
                                                <span className='flex-shrink-0 size-5 bg-blue-200 text-blue-800 rounded-full flex items-center justify-center text-xs font-medium'>
                                                    {step.new.order}
                                                </span>
                                                Step {step.new.order}
                                            </div>
                                            <div className='space-y-1 text-xs'>
                                                <div className='bg-red-100 text-red-700 px-2 py-1 rounded'>
                                                    <span className='font-medium'>Before:</span> {step.old.details}
                                                </div>
                                                <div className='bg-green-100 text-green-700 px-2 py-1 rounded'>
                                                    <span className='font-medium'>After:</span> {step.new.details}
                                                </div>
                                            </div>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {repositionedSteps.length > 0 && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-purple-700'>
                                <svg className='size-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'>
                                    <path strokeLinecap='round' strokeLinejoin='round' strokeWidth={2} d='M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4' />
                                </svg>
                                <span className='font-medium'>Repositioned ({repositionedSteps.length})</span>
                            </div>
                            <ul className='space-y-2 ml-6'>
                                {repositionedSteps.map((step, index) => (
                                    <li key={index} className='text-sm bg-purple-50 text-purple-800 px-3 py-2 rounded'>
                                        <div className='space-y-1'>
                                            <div className='flex items-start gap-2'>
                                                <span className='flex-shrink-0 size-5 bg-purple-200 text-purple-800 rounded-full flex items-center justify-center text-xs font-medium'>
                                                    {step.new.order}
                                                </span>
                                                <span>{step.new.details}</span>
                                            </div>
                                            <div className='text-xs text-purple-600 ml-7'>
                                                Moved from position {step.old.order} to {step.new.order}
                                            </div>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {hasReordering && (
                        <div className='space-y-2'>
                            <div className='flex items-center gap-2 text-amber-700'>
                                <svg className='size-4' fill='none' stroke='currentColor' viewBox='0 0 24 24'>
                                    <path strokeLinecap='round' strokeLinejoin='round' strokeWidth={2} d='M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4' />
                                </svg>
                                <span className='font-medium'>Reordered</span>
                            </div>
                            <div className='text-sm bg-amber-50 text-amber-800 px-3 py-2 rounded ml-6'>
                                Instructions have been reordered. See the before/after comparison below for details.
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    const renderServingSizeChanges = () => {
        if (!changes) return null;

        const oldSize = changes.oldDetails.servingSize;
        const newSize = changes.currentDetails.servingSize;

        if (oldSize === newSize) return null;

        return (
            <div className='flex items-center gap-4'>
                <div className='flex items-center gap-2'>
                    <Minus className='size-4 text-red-600' />
                    <span className='bg-red-50 text-red-800 px-2 py-1 rounded text-sm'>
                        {oldSize} servings
                    </span>
                </div>
                <div className='flex items-center gap-2'>
                    <Plus className='size-4 text-green-600' />
                    <span className='bg-green-50 text-green-800 px-2 py-1 rounded text-sm'>
                        {newSize} servings
                    </span>
                </div>
            </div>
        );
    };

    const hasServingSizeChanges = () => {
        return changes && changes.oldDetails.servingSize !== changes.currentDetails.servingSize;
    };

    const hasIngredientChanges = () => {
        if (!changes) return false;

        const oldIngredients = changes.oldDetails.recipeIngredients || [];
        const newIngredients = changes.currentDetails.recipeIngredients || [];

        // Find completely new ingredients (by name)
        const addedIngredients = newIngredients.filter(newIng =>
            !oldIngredients.find(oldIng => oldIng.name.toLowerCase() === newIng.name.toLowerCase())
        );

        // Find completely removed ingredients (by name)
        const removedIngredients = oldIngredients.filter(oldIng =>
            !newIngredients.find(newIng => newIng.name.toLowerCase() === oldIng.name.toLowerCase())
        );

        // Find modified ingredients (same name, different amount/unit)
        const modifiedIngredients = newIngredients.filter(newIng => {
            const oldIng = oldIngredients.find(old => old.name.toLowerCase() === newIng.name.toLowerCase());
            return oldIng && (oldIng.amount !== newIng.amount || oldIng.unit !== newIng.unit);
        });

        // Check for reordering (same ingredients but different order)
        const hasReordering = JSON.stringify(oldIngredients.map(i => `${i.name}|${i.amount}|${i.unit}`))
            !== JSON.stringify(newIngredients.map(i => `${i.name}|${i.amount}|${i.unit}`)) &&
            addedIngredients.length === 0 && removedIngredients.length === 0 && modifiedIngredients.length === 0;

        return addedIngredients.length > 0 || removedIngredients.length > 0 || modifiedIngredients.length > 0 || hasReordering;
    };

    const hasStepChanges = () => {
        if (!changes) return false;

        const oldSteps = changes.oldDetails.recipeSteps || [];
        const newSteps = changes.currentDetails.recipeSteps || [];

        // Find completely new steps (by content)
        const addedSteps = newSteps.filter(newStep =>
            !oldSteps.find(oldStep => oldStep.details.toLowerCase().trim() === newStep.details.toLowerCase().trim())
        );

        // Find completely removed steps (by content)
        const removedSteps = oldSteps.filter(oldStep =>
            !newSteps.find(newStep => newStep.details.toLowerCase().trim() === oldStep.details.toLowerCase().trim())
        );

        // Find repositioned steps (same content but different position)
        const repositionedSteps = newSteps.filter(newStep => {
            const oldStep = oldSteps.find(old => old.details.toLowerCase().trim() === newStep.details.toLowerCase().trim());
            return oldStep && oldStep.order !== newStep.order;
        });

        // Check for content modifications (same order but different content)
        const modifiedSteps = newSteps.filter(newStep => {
            const oldStep = oldSteps.find(old => old.order === newStep.order);
            return oldStep && oldStep.details.toLowerCase().trim() !== newStep.details.toLowerCase().trim() &&
                !addedSteps.find(added => added.order === newStep.order) &&
                !removedSteps.find(removed => removed.order === oldStep.order);
        });

        // Check for pure reordering (same steps but different order)
        const hasReordering = JSON.stringify(oldSteps.map(s => s.details.toLowerCase().trim()).sort()) ===
            JSON.stringify(newSteps.map(s => s.details.toLowerCase().trim()).sort()) &&
            JSON.stringify(oldSteps.map(s => `${s.order}|${s.details}`)) !==
            JSON.stringify(newSteps.map(s => `${s.order}|${s.details}`)) &&
            addedSteps.length === 0 && removedSteps.length === 0 && modifiedSteps.length === 0;

        return addedSteps.length > 0 || removedSteps.length > 0 || repositionedSteps.length > 0 || modifiedSteps.length > 0 || hasReordering;
    };

    if (isLoading) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-4xl'>
                    <div className='animate-pulse'>
                        <div className='mb-6 h-8 w-1/3 rounded bg-gray-200'></div>
                        <div className='mb-8 h-24 rounded-lg bg-gray-200'></div>
                        <div className='space-y-6'>
                            <div className='h-32 rounded-lg bg-gray-200'></div>
                            <div className='h-32 rounded-lg bg-gray-200'></div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (!changes || !commitDetails) {
        return (
            <div className='container mx-auto px-4 py-8'>
                <div className='mx-auto max-w-4xl text-center'>
                    <h1 className='mb-4 text-2xl font-bold text-gray-900'>Changes Not Found</h1>
                    <p className='mb-8 text-gray-600'>
                        The commit changes you&apos;re looking for couldn&apos;t be loaded.
                    </p>
                    <Button onClick={handleBackToHistory} className='bg-[#FF7C75] hover:bg-[#FF7C75]/90'>
                        Back to History
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className='container mx-auto px-4 py-6'>
            <div className='mx-auto max-w-4xl'>
                {/* Header */}
                <div className='mb-6'>
                    <Button
                        onClick={handleBackToHistory}
                        variant='outline'
                        size='sm'
                        className='mb-4 border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white'
                    >
                        <ArrowLeft className='mr-2 size-4' />
                        Back to History
                    </Button>

                    {/* Commit info */}
                    <div className='rounded-lg border border-gray-200 p-6'>
                        <div className='flex items-start gap-4'>
                            <div className='flex size-12 items-center justify-center rounded-full bg-[#FF7C75] text-white'>
                                <GitCommit className='size-6' />
                            </div>

                            <div className='flex-1'>
                                <h1 className='text-2xl font-bold text-gray-900 mb-2'>
                                    {commitDetails.commitMetadata.message}
                                </h1>

                                <div className='flex items-center gap-6 text-sm text-gray-600 mb-3'>
                                    <div className='flex items-center gap-2'>
                                        <BarChart3 className='size-4' />
                                        <span>
                                            {(() => {
                                                const changeCounts = calculateChanges();
                                                const parts = [];
                                                if (changeCounts.additions > 0) parts.push(`+${changeCounts.additions} additions`);
                                                if (changeCounts.deletions > 0) parts.push(`-${changeCounts.deletions} deletions`);
                                                if (changeCounts.modifications > 0) parts.push(`${changeCounts.modifications} modifications`);
                                                return parts.join(', ') || 'No changes';
                                            })()}
                                        </span>
                                    </div>
                                    <div className='flex items-center gap-2'>
                                        <Calendar className='size-4' />
                                        <span>{formatDate(commitDetails.commitMetadata.createdAt)}</span>
                                    </div>
                                    <Badge variant='outline' className='font-mono text-[#FF7C75] border-[#FF7C75]'>
                                        {formatCommitId(commitDetails.commitMetadata.id)}
                                    </Badge>
                                    {changes.firstCommit && (
                                        <Badge className='bg-blue-100 text-blue-700 hover:bg-blue-200'>
                                            Initial Commit
                                        </Badge>
                                    )}
                                </div>

                                {/* User info */}
                                <div className='flex items-center gap-2'>
                                    <span className='text-sm text-gray-600'>By:</span>
                                    <UserInfo
                                        userId={commitDetails.commitMetadata.userId}
                                        size='sm'
                                        clickable={true}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Initial commit notice */}
                {changes.firstCommit && (
                    <div className='mb-6'>
                        <div className='rounded-lg bg-blue-50 border border-blue-200 p-4'>
                            <div className='flex items-center gap-2 text-blue-800'>
                                <GitCommit className='size-4' />
                                <span className='font-medium'>This is the initial commit for this recipe.</span>
                            </div>
                            <p className='text-blue-700 text-sm mt-1'>
                                All content shown below was added when the recipe was first created.
                            </p>
                        </div>
                    </div>
                )}

                {/* Changes */}
                <div className='space-y-6 mb-6'>
                    <div className='flex items-center gap-2'>
                        <h2 className='text-xl font-semibold text-gray-900'>Changes</h2>
                        <TooltipProvider delayDuration={0}>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button className='inline-flex items-center justify-center rounded-full text-gray-400 hover:text-gray-600 transition-colors'>
                                        <Info className='size-4' />
                                    </button>
                                </TooltipTrigger>
                                <TooltipContent side="right" className='max-w-xs'>
                                    <div className='space-y-2'>
                                        <p className='font-medium'>Color Legend:</p>
                                        <div className='space-y-1 text-xs'>
                                            <div className='flex items-center gap-2'>
                                                <div className='w-3 h-3 bg-red-500 rounded-full'></div>
                                                <span>Red - Removed/Deleted</span>
                                            </div>
                                            <div className='flex items-center gap-2'>
                                                <div className='w-3 h-3 bg-green-500 rounded-full'></div>
                                                <span>Green - Added/New</span>
                                            </div>
                                            <div className='flex items-center gap-2'>
                                                <div className='w-3 h-3 bg-blue-500 rounded-full'></div>
                                                <span>Blue - Modified/Changed</span>
                                            </div>
                                            <div className='flex items-center gap-2'>
                                                <div className='w-3 h-3 bg-amber-500 rounded-full'></div>
                                                <span>Amber - Moved/Repositioned</span>
                                            </div>
                                            <div className='flex items-center gap-2'>
                                                <div className='w-3 h-3 bg-purple-500 rounded-full'></div>
                                                <span>Purple - Repositioned</span>
                                            </div>
                                        </div>
                                    </div>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </div>

                    <div className='space-y-6'>
                        {/* Serving size changes */}
                        {hasServingSizeChanges() && (
                            <div className='space-y-3'>
                                <h3 className='text-lg font-semibold text-gray-900'>Serving Size</h3>
                                <div className='rounded-lg border border-gray-200 p-4'>
                                    {renderServingSizeChanges()}
                                </div>
                            </div>
                        )}

                        {/* Ingredient changes */}
                        {hasIngredientChanges() && renderIngredientChanges()}

                        {/* Step changes */}
                        {hasStepChanges() && renderStepChanges()}

                        {/* Show message if no changes at all */}
                        {!hasServingSizeChanges() && !hasIngredientChanges() && !hasStepChanges() && (
                            <div className='text-center py-8'>
                                <p className='text-gray-500 text-lg'>No changes found in this commit.</p>
                            </div>
                        )}

                        {/* Before/After Comparison */}
                        {!changes.firstCommit && (
                            <div className='space-y-4 mt-8 pt-6 border-t border-gray-200'>
                                <div className='flex items-center gap-2'>
                                    <h2 className='text-xl font-semibold text-gray-900'>Before / After Comparison</h2>
                                    <TooltipProvider delayDuration={0}>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <button className='inline-flex items-center justify-center rounded-full text-gray-400 hover:text-gray-600 transition-colors'>
                                                    <Info className='size-4' />
                                                </button>
                                            </TooltipTrigger>
                                            <TooltipContent side="right" className='max-w-xs'>
                                                <div className='space-y-2'>
                                                    <p className='font-medium'>Color Legend:</p>
                                                    <div className='space-y-1 text-xs'>
                                                        <div className='flex items-center gap-2'>
                                                            <div className='w-3 h-3 bg-red-500 rounded-full'></div>
                                                            <span>Red - Before (Removed/Deleted)</span>
                                                        </div>
                                                        <div className='flex items-center gap-2'>
                                                            <div className='w-3 h-3 bg-green-500 rounded-full'></div>
                                                            <span>Green - After (Added/New)</span>
                                                        </div>
                                                        <div className='flex items-center gap-2'>
                                                            <div className='w-3 h-3 bg-blue-500 rounded-full'></div>
                                                            <span>Blue - Modified/Changed</span>
                                                        </div>
                                                        <div className='flex items-center gap-2'>
                                                            <div className='w-3 h-3 bg-amber-500 rounded-full'></div>
                                                            <span>Amber - Moved/Repositioned</span>
                                                        </div>
                                                        <div className='flex items-center gap-2'>
                                                            <div className='w-3 h-3 bg-purple-500 rounded-full'></div>
                                                            <span>Purple - Repositioned</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                </div>

                                <div className='space-y-6'>
                                    {/* Column Headers */}
                                    <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
                                        <div className='flex items-center gap-2'>
                                            <div className='w-3 h-3 bg-red-500 rounded-full'></div>
                                            <h3 className='text-lg font-medium text-gray-800'>Before</h3>
                                        </div>
                                        <div className='flex items-center gap-2'>
                                            <div className='w-3 h-3 bg-green-500 rounded-full'></div>
                                            <h3 className='text-lg font-medium text-gray-800'>After</h3>
                                        </div>
                                    </div>

                                    {/* Serving Size Row */}
                                    <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
                                        <div className='bg-slate-50 rounded-lg p-4 border-l-4 border-red-300 border border-gray-200'>
                                            <h4 className='font-medium text-gray-700 mb-2'>Serving Size</h4>
                                            <p className={`text-sm ${changes.oldDetails.servingSize !== changes.currentDetails.servingSize ? 'bg-red-100 text-red-800 px-2 py-1 rounded' : 'text-gray-600'}`}>
                                                {changes.oldDetails.servingSize} servings
                                            </p>
                                        </div>
                                        <div className='bg-white rounded-lg p-4 border-l-4 border-green-300 border border-gray-200'>
                                            <h4 className='font-medium text-gray-700 mb-2'>Serving Size</h4>
                                            <p className={`text-sm ${changes.oldDetails.servingSize !== changes.currentDetails.servingSize ? 'bg-green-100 text-green-800 px-2 py-1 rounded' : 'text-gray-600'}`}>
                                                {changes.currentDetails.servingSize} servings
                                            </p>
                                        </div>
                                    </div>

                                    {/* Ingredients Row */}
                                    {((changes.oldDetails.recipeIngredients && changes.oldDetails.recipeIngredients.length > 0) ||
                                        (changes.currentDetails.recipeIngredients && changes.currentDetails.recipeIngredients.length > 0)) && (
                                            <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
                                                {/* Before Ingredients */}
                                                <div className='bg-slate-50 rounded-lg p-4 border-l-4 border-red-300 border border-gray-200'>
                                                    <h4 className='font-medium text-gray-700 mb-3 flex items-center gap-2'>
                                                        <ChefHat className='size-4' />
                                                        Ingredients
                                                    </h4>
                                                    {changes.oldDetails.recipeIngredients && changes.oldDetails.recipeIngredients.length > 0 ? (
                                                        <ol className='space-y-2'>
                                                            {changes.oldDetails.recipeIngredients.map((ingredient, index) => {
                                                                // Check if ingredient is completely removed (by name)
                                                                const isRemoved = !changes.currentDetails.recipeIngredients?.find(newIng =>
                                                                    newIng.name.toLowerCase() === ingredient.name.toLowerCase()
                                                                );

                                                                // Check if ingredient moved positions (same name+amount+unit, different position)
                                                                const newPosition = changes.currentDetails.recipeIngredients?.findIndex(newIng =>
                                                                    newIng.name.toLowerCase() === ingredient.name.toLowerCase() &&
                                                                    newIng.amount === ingredient.amount && newIng.unit === ingredient.unit
                                                                );
                                                                const hasMoved = !isRemoved && newPosition !== undefined && newPosition !== -1 && newPosition !== index;

                                                                // Check if ingredient is modified (same name, different amount/unit)
                                                                const matchingIngredient = changes.currentDetails.recipeIngredients?.find(newIng =>
                                                                    newIng.name.toLowerCase() === ingredient.name.toLowerCase()
                                                                );
                                                                const isModified = !isRemoved && !hasMoved && matchingIngredient &&
                                                                    (matchingIngredient.amount !== ingredient.amount || matchingIngredient.unit !== ingredient.unit);

                                                                let className = 'text-sm flex items-start gap-2 p-2 rounded';
                                                                let indicator = '';

                                                                if (isRemoved) {
                                                                    className += ' bg-red-100 text-red-800 border border-red-200';
                                                                } else if (isModified) {
                                                                    className += ' bg-blue-100 text-blue-800 border border-blue-200';
                                                                    indicator = 'modified';
                                                                } else if (hasMoved) {
                                                                    className += ' bg-amber-100 text-amber-800 border border-amber-200';
                                                                    indicator = 'moved';
                                                                } else {
                                                                    className += ' text-gray-600';
                                                                }

                                                                return (
                                                                    <li key={index} className={className}>
                                                                        <span className='flex-shrink-0 w-6 h-6 bg-gray-200 text-gray-700 rounded-full flex items-center justify-center text-xs font-medium'>
                                                                            {index + 1}
                                                                        </span>
                                                                        <div className='flex-1'>
                                                                            <span>{ingredient.amount} {ingredient.unit} {ingredient.name}</span>
                                                                            {indicator === 'moved' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    → Moved to position {(newPosition as number) + 1}
                                                                                </div>
                                                                            )}
                                                                            {indicator === 'modified' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    → Modified
                                                                                </div>
                                                                            )}
                                                                        </div>
                                                                    </li>
                                                                );
                                                            })}
                                                        </ol>
                                                    ) : (
                                                        <p className='text-sm text-gray-500 italic'>No ingredients</p>
                                                    )}
                                                </div>

                                                {/* After Ingredients */}
                                                <div className='bg-white rounded-lg p-4 border-l-4 border-green-300 border border-gray-200'>
                                                    <h4 className='font-medium text-gray-700 mb-3 flex items-center gap-2'>
                                                        <ChefHat className='size-4' />
                                                        Ingredients
                                                    </h4>
                                                    {changes.currentDetails.recipeIngredients && changes.currentDetails.recipeIngredients.length > 0 ? (
                                                        <ol className='space-y-2'>
                                                            {changes.currentDetails.recipeIngredients.map((ingredient, index) => {
                                                                // Check if ingredient is completely new (by name)
                                                                const isNew = !changes.oldDetails.recipeIngredients?.find(oldIng =>
                                                                    oldIng.name.toLowerCase() === ingredient.name.toLowerCase()
                                                                );

                                                                // Check if ingredient moved positions (same name+amount+unit, different position)
                                                                const oldPosition = changes.oldDetails.recipeIngredients?.findIndex(oldIng =>
                                                                    oldIng.name.toLowerCase() === ingredient.name.toLowerCase() &&
                                                                    oldIng.amount === ingredient.amount && oldIng.unit === ingredient.unit
                                                                );
                                                                const hasMoved = !isNew && oldPosition !== undefined && oldPosition !== -1 && oldPosition !== index;

                                                                // Check if ingredient is modified (same name, different amount/unit)
                                                                const matchingIngredient = changes.oldDetails.recipeIngredients?.find(oldIng =>
                                                                    oldIng.name.toLowerCase() === ingredient.name.toLowerCase()
                                                                );
                                                                const isModified = !isNew && !hasMoved && matchingIngredient &&
                                                                    (matchingIngredient.amount !== ingredient.amount || matchingIngredient.unit !== ingredient.unit);

                                                                let className = 'text-sm flex items-start gap-2 p-2 rounded';
                                                                let indicator = '';

                                                                if (isNew) {
                                                                    className += ' bg-green-100 text-green-800 border border-green-200';
                                                                } else if (isModified) {
                                                                    className += ' bg-blue-100 text-blue-800 border border-blue-200';
                                                                    indicator = 'modified';
                                                                } else if (hasMoved) {
                                                                    className += ' bg-amber-100 text-amber-800 border border-amber-200';
                                                                    indicator = 'moved';
                                                                } else {
                                                                    className += ' text-gray-600';
                                                                }

                                                                return (
                                                                    <li key={index} className={className}>
                                                                        <span className='flex-shrink-0 w-6 h-6 bg-gray-200 text-gray-700 rounded-full flex items-center justify-center text-xs font-medium'>
                                                                            {index + 1}
                                                                        </span>
                                                                        <div className='flex-1'>
                                                                            <span>{ingredient.amount} {ingredient.unit} {ingredient.name}</span>
                                                                            {indicator === 'moved' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    ← Moved from position {(oldPosition as number) + 1}
                                                                                </div>
                                                                            )}
                                                                            {indicator === 'modified' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    ← Modified from previous version
                                                                                </div>
                                                                            )}
                                                                        </div>
                                                                    </li>
                                                                );
                                                            })}
                                                        </ol>
                                                    ) : (
                                                        <p className='text-sm text-gray-500 italic'>No ingredients</p>
                                                    )}
                                                </div>
                                            </div>
                                        )}

                                    {/* Instructions Row */}
                                    {((changes.oldDetails.recipeSteps && changes.oldDetails.recipeSteps.length > 0) ||
                                        (changes.currentDetails.recipeSteps && changes.currentDetails.recipeSteps.length > 0)) && (
                                            <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
                                                {/* Before Steps */}
                                                <div className='bg-slate-50 rounded-lg p-4 border-l-4 border-red-300 border border-gray-200'>
                                                    <h4 className='font-medium text-gray-700 mb-3 flex items-center gap-2'>
                                                        <FileText className='size-4' />
                                                        Instructions
                                                    </h4>
                                                    {changes.oldDetails.recipeSteps && changes.oldDetails.recipeSteps.length > 0 ? (
                                                        <ol className='space-y-2'>
                                                            {changes.oldDetails.recipeSteps.map((step, index) => {
                                                                // Check if instruction is completely removed (by content)
                                                                const isRemoved = !changes.currentDetails.recipeSteps?.find(newStep =>
                                                                    newStep.details.toLowerCase().trim() === step.details.toLowerCase().trim()
                                                                );

                                                                // Check if instruction moved positions (same content, different position)
                                                                const newPosition = changes.currentDetails.recipeSteps?.findIndex(newStep =>
                                                                    newStep.details.toLowerCase().trim() === step.details.toLowerCase().trim()
                                                                );
                                                                const hasMoved = !isRemoved && newPosition !== undefined && newPosition !== -1 && newPosition !== (step.order - 1);

                                                                // Check if instruction is modified (same position but different content)
                                                                const currentStepAtSamePosition = changes.currentDetails.recipeSteps?.find(newStep =>
                                                                    newStep.order === step.order
                                                                );
                                                                const isModified = !isRemoved && !hasMoved && currentStepAtSamePosition &&
                                                                    currentStepAtSamePosition.details.toLowerCase().trim() !== step.details.toLowerCase().trim();

                                                                let className = 'text-sm flex items-start gap-2 p-2 rounded';
                                                                let indicator = '';

                                                                if (isRemoved) {
                                                                    className += ' bg-red-100 text-red-800 border border-red-200';
                                                                } else if (isModified) {
                                                                    className += ' bg-blue-100 text-blue-800 border border-blue-200';
                                                                    indicator = 'modified';
                                                                } else if (hasMoved) {
                                                                    className += ' bg-amber-100 text-amber-800 border border-amber-200';
                                                                    indicator = 'moved';
                                                                } else {
                                                                    className += ' text-gray-600';
                                                                }

                                                                return (
                                                                    <li key={index} className={className}>
                                                                        <span className='flex-shrink-0 w-6 h-6 bg-gray-200 text-gray-700 rounded-full flex items-center justify-center text-xs font-medium'>
                                                                            {step.order}
                                                                        </span>
                                                                        <div className='flex-1'>
                                                                            <span>{step.details}</span>
                                                                            {indicator === 'moved' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    → Moved to position {(newPosition as number) + 1}
                                                                                </div>
                                                                            )}
                                                                            {indicator === 'modified' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    → Modified
                                                                                </div>
                                                                            )}
                                                                        </div>
                                                                    </li>
                                                                );
                                                            })}
                                                        </ol>
                                                    ) : (
                                                        <p className='text-sm text-gray-500 italic'>No instructions</p>
                                                    )}
                                                </div>

                                                {/* After Steps */}
                                                <div className='bg-white rounded-lg p-4 border-l-4 border-green-300 border border-gray-200'>
                                                    <h4 className='font-medium text-gray-700 mb-3 flex items-center gap-2'>
                                                        <FileText className='size-4' />
                                                        Instructions
                                                    </h4>
                                                    {changes.currentDetails.recipeSteps && changes.currentDetails.recipeSteps.length > 0 ? (
                                                        <ol className='space-y-2'>
                                                            {changes.currentDetails.recipeSteps.map((step, index) => {
                                                                // Check if instruction is completely new (by content)
                                                                const isNew = !changes.oldDetails.recipeSteps?.find(oldStep =>
                                                                    oldStep.details.toLowerCase().trim() === step.details.toLowerCase().trim()
                                                                );

                                                                // Check if instruction moved positions (same content, different position)
                                                                const oldPosition = changes.oldDetails.recipeSteps?.findIndex(oldStep =>
                                                                    oldStep.details.toLowerCase().trim() === step.details.toLowerCase().trim()
                                                                );
                                                                const hasMoved = !isNew && oldPosition !== undefined && oldPosition !== -1 && oldPosition !== (step.order - 1);

                                                                // Check if instruction is modified (same position but different content)
                                                                const oldStepAtSamePosition = changes.oldDetails.recipeSteps?.find(oldStep =>
                                                                    oldStep.order === step.order
                                                                );
                                                                const isModified = !isNew && !hasMoved && oldStepAtSamePosition &&
                                                                    oldStepAtSamePosition.details.toLowerCase().trim() !== step.details.toLowerCase().trim();

                                                                let className = 'text-sm flex items-start gap-2 p-2 rounded';
                                                                let indicator = '';

                                                                if (isNew) {
                                                                    className += ' bg-green-100 text-green-800 border border-green-200';
                                                                } else if (isModified) {
                                                                    className += ' bg-blue-100 text-blue-800 border border-blue-200';
                                                                    indicator = 'modified';
                                                                } else if (hasMoved) {
                                                                    className += ' bg-amber-100 text-amber-800 border border-amber-200';
                                                                    indicator = 'moved';
                                                                } else {
                                                                    className += ' text-gray-600';
                                                                }

                                                                return (
                                                                    <li key={index} className={className}>
                                                                        <span className='flex-shrink-0 w-6 h-6 bg-gray-200 text-gray-700 rounded-full flex items-center justify-center text-xs font-medium'>
                                                                            {step.order}
                                                                        </span>
                                                                        <div className='flex-1'>
                                                                            <span>{step.details}</span>
                                                                            {indicator === 'moved' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    ← Moved from position {(oldPosition as number) + 1}
                                                                                </div>
                                                                            )}
                                                                            {indicator === 'modified' && (
                                                                                <div className='text-xs mt-1'>
                                                                                    ← Modified from previous version
                                                                                </div>
                                                                            )}
                                                                        </div>
                                                                    </li>
                                                                );
                                                            })}
                                                        </ol>
                                                    ) : (
                                                        <p className='text-sm text-gray-500 italic'>No instructions</p>
                                                    )}
                                                </div>
                                            </div>
                                        )}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
} 