'use client';

import { Button } from '@/components/ui/button';
import { Copy, Edit, Trash2, Loader2 } from 'lucide-react';

interface RecipeActionButtonsProps {
    isOwner: boolean;
    onCopy: () => void;
    onEdit: () => void;
    onDelete: () => void;
    isDeleting?: boolean;
    isCloning?: boolean;
}

export function RecipeActionButtons({
    isOwner,
    onCopy,
    onEdit,
    onDelete,
    isDeleting = false,
    isCloning = false,
}: RecipeActionButtonsProps) {
    return (
        <div className="flex items-center justify-end gap-3 mb-8">
            <Button
                onClick={onCopy}
                variant="outline"
                size="sm"
                disabled={isCloning}
                className="border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white transition-colors disabled:opacity-50"
            >
                {isCloning ? (
                    <Loader2 className="mr-2 size-4 animate-spin" />
                ) : (
                    <Copy className="mr-2 size-4" />
                )}
                {isCloning ? 'Cloning...' : 'Copy'}
            </Button>

            {isOwner && (
                <>
                    <Button
                        onClick={onEdit}
                        variant="outline"
                        size="sm"
                        disabled={isDeleting || isCloning}
                        className="border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white transition-colors disabled:opacity-50"
                    >
                        <Edit className="mr-2 size-4" />
                        Edit
                    </Button>

                    <Button
                        onClick={onDelete}
                        variant="outline"
                        size="sm"
                        disabled={isDeleting || isCloning}
                        className="border-red-500 text-red-500 hover:bg-red-500 hover:text-white transition-colors disabled:opacity-50"
                    >
                        {isDeleting ? (
                            <Loader2 className="mr-2 size-4 animate-spin" />
                        ) : (
                            <Trash2 className="mr-2 size-4" />
                        )}
                        {isDeleting ? 'Deleting...' : 'Delete'}
                    </Button>
                </>
            )}
        </div>
    );
} 