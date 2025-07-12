

import { Button } from '@/components/ui/button';
import { Copy, Edit, Trash2, Loader2, History } from 'lucide-react';
import { BranchDropdown } from './branch-dropdown';
import { BranchDTO } from '@/lib/types/recipe';

interface RecipeActionButtonsProps {
  recipeId: number;
  currentBranch: BranchDTO | null;
  onBranchChange: (branch: BranchDTO) => void;
  onHistoryClick: () => void;
  isOwner: boolean;
  onCopy: () => void;
  onEdit: () => void;
  onDelete: () => void;
  isDeleting?: boolean;
  isCloning?: boolean;
}

export function RecipeActionButtons({
  recipeId,
  currentBranch,
  onBranchChange,
  onHistoryClick,
  isOwner,
  onCopy,
  onEdit,
  onDelete,
  isDeleting = false,
  isCloning = false,
}: RecipeActionButtonsProps) {
  return (
    <div className='mb-8 flex items-center justify-between'>
      {/* Branch dropdown and history button on the left */}
      <div className='flex items-center gap-3'>
        <BranchDropdown
          recipeId={recipeId}
          currentBranch={currentBranch}
          onBranchChange={onBranchChange}
          isOwner={isOwner}
        />

        <Button
          onClick={onHistoryClick}
          variant='outline'
          size='sm'
          className='border-[#FF7C75] text-[#FF7C75] transition-colors hover:bg-[#FF7C75] hover:text-white'
        >
          <History className='mr-2 size-4' />
          History
        </Button>
      </div>

      {/* Action buttons on the right */}
      <div className='flex items-center gap-3'>
        <Button
          onClick={onCopy}
          variant='outline'
          size='sm'
          disabled={isCloning}
          className='border-[#FF7C75] text-[#FF7C75] transition-colors hover:bg-[#FF7C75] hover:text-white disabled:opacity-50'
        >
          {isCloning ? <Loader2 className='mr-2 size-4 animate-spin' /> : <Copy className='mr-2 size-4' />}
          {isCloning ? 'Cloning...' : 'Copy'}
        </Button>

        {isOwner && (
          <>
            <Button
              onClick={onEdit}
              variant='outline'
              size='sm'
              disabled={isDeleting || isCloning}
              className='border-[#FF7C75] text-[#FF7C75] transition-colors hover:bg-[#FF7C75] hover:text-white disabled:opacity-50'
            >
              <Edit className='mr-2 size-4' />
              Edit
            </Button>

            <Button
              onClick={onDelete}
              variant='outline'
              size='sm'
              disabled={isDeleting || isCloning}
              className='border-red-500 text-red-500 transition-colors hover:bg-red-500 hover:text-white disabled:opacity-50'
            >
              {isDeleting ? <Loader2 className='mr-2 size-4 animate-spin' /> : <Trash2 className='mr-2 size-4' />}
              {isDeleting ? 'Deleting...' : 'Delete'}
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
