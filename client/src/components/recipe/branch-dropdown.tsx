import { useState, useEffect, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ChevronDown, GitBranch, Plus, X } from 'lucide-react';
import { toast } from 'sonner';
import { getRecipeBranches, createBranch } from '@/lib/services/recipeService';
import { BranchDTO } from '@/lib/types/recipe';

interface BranchDropdownProps {
    recipeId: number;
    currentBranch: BranchDTO | null;
    onBranchChange: (branch: BranchDTO) => void;
    isOwner: boolean;
}

export function BranchDropdown({ recipeId, currentBranch, onBranchChange, isOwner }: BranchDropdownProps) {
    const [branches, setBranches] = useState<BranchDTO[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    const [newBranchName, setNewBranchName] = useState('');
    const [isCreating, setIsCreating] = useState(false);

    const dropdownRef = useRef<HTMLDivElement>(null);

    // Fetch branches when component mounts
    useEffect(() => {
        fetchBranches();
    }, [recipeId]);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const fetchBranches = async () => {
        try {
            setIsLoading(true);
            const branchList = await getRecipeBranches(recipeId);
            setBranches(branchList);
        } catch (error) {
            console.error('Failed to fetch branches:', error);
            toast.error('Failed to load branches');
        } finally {
            setIsLoading(false);
        }
    };

    const handleBranchSelect = (branch: BranchDTO) => {
        onBranchChange(branch);
        setIsOpen(false);
    };

    const handleCreateBranch = async () => {
        if (!newBranchName.trim()) {
            toast.error('Branch name is required');
            return;
        }

        if (!currentBranch) {
            toast.error('No current branch selected');
            return;
        }

        // Check if branch name already exists
        if (branches.some(branch => branch.name.toLowerCase() === newBranchName.trim().toLowerCase())) {
            toast.error('Branch name already exists');
            return;
        }

        try {
            setIsCreating(true);
            const newBranch = await createBranch(recipeId, newBranchName.trim(), currentBranch.id);

            // Update branches list
            setBranches([...branches, newBranch]);

            // Select the new branch
            onBranchChange(newBranch);

            // Close dialog and reset form
            setShowCreateDialog(false);
            setNewBranchName('');
            setIsOpen(false);

            toast.success(`Branch "${newBranch.name}" created successfully`);
        } catch (error) {
            console.error('Failed to create branch:', error);
            toast.error('Failed to create branch');
        } finally {
            setIsCreating(false);
        }
    };

    const handleCancelCreate = () => {
        setShowCreateDialog(false);
        setNewBranchName('');
    };

    return (
        <div className="relative" ref={dropdownRef}>
            {/* Main dropdown button */}
            <Button
                variant="outline"
                size="sm"
                onClick={() => setIsOpen(!isOpen)}
                disabled={isLoading}
                className="border-[#FF7C75] text-[#FF7C75] transition-colors hover:bg-[#FF7C75] hover:text-white min-w-[140px] justify-between"
            >
                <div className="flex items-center gap-2">
                    <GitBranch className="size-4" />
                    <span className="truncate">
                        {currentBranch?.name || 'Select Branch'}
                    </span>
                </div>
                <ChevronDown className={`size-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </Button>

            {/* Dropdown menu */}
            {isOpen && (
                <div className="absolute top-full left-0 mt-1 w-64 bg-white border border-gray-200 rounded-md shadow-lg z-50">
                    {/* Create branch button for owners */}
                    {isOwner && (
                        <div className="p-2 border-b border-gray-100">
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setShowCreateDialog(true)}
                                className="w-full justify-start text-[#FF7C75] hover:bg-[#FF7C75]/10"
                            >
                                <Plus className="size-4 mr-2" />
                                Create Branch
                            </Button>
                        </div>
                    )}

                    {/* Branches list */}
                    <ScrollArea className="max-h-[200px]">
                        <div className="p-1">
                            {branches.length === 0 ? (
                                <div className="p-3 text-center text-gray-500 text-sm">
                                    {isLoading ? 'Loading branches...' : 'No branches available'}
                                </div>
                            ) : (
                                branches.map((branch) => (
                                    <button
                                        key={branch.id}
                                        onClick={() => handleBranchSelect(branch)}
                                        className={`w-full text-left p-2 rounded-md hover:bg-gray-50 transition-colors ${currentBranch?.id === branch.id ? 'bg-[#FF7C75]/10 text-[#FF7C75]' : 'text-gray-700'
                                            }`}
                                    >
                                        <div className="flex items-center gap-2">
                                            <GitBranch className="size-4" />
                                            <span className="truncate">{branch.name}</span>
                                        </div>
                                    </button>
                                ))
                            )}
                        </div>
                    </ScrollArea>
                </div>
            )}

            {/* Create branch dialog */}
            {showCreateDialog && (
                <div className="fixed inset-0 z-50 flex items-center justify-center">
                    <div
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm"
                        onClick={handleCancelCreate}
                    />
                    <div className="relative bg-white rounded-lg p-6 w-full max-w-md mx-4 shadow-xl">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-lg font-semibold text-gray-900">Create New Branch</h3>
                            <button
                                onClick={handleCancelCreate}
                                className="text-gray-400 hover:text-gray-600 transition-colors"
                            >
                                <X className="size-5" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <Label htmlFor="branch-name">Branch Name</Label>
                                <Input
                                    id="branch-name"
                                    value={newBranchName}
                                    onChange={(e) => setNewBranchName(e.target.value)}
                                    placeholder="Enter branch name"
                                    className="mt-1"
                                    disabled={isCreating}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter' && !isCreating) {
                                            handleCreateBranch();
                                        }
                                    }}
                                />
                            </div>

                            <div className="flex justify-end gap-3">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={handleCancelCreate}
                                    disabled={isCreating}
                                >
                                    Cancel
                                </Button>
                                <Button
                                    size="sm"
                                    onClick={handleCreateBranch}
                                    disabled={isCreating || !newBranchName.trim()}
                                    className="bg-[#FF7C75] hover:bg-[#FF7C75]/90"
                                >
                                    {isCreating ? 'Creating...' : 'Create Branch'}
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
} 