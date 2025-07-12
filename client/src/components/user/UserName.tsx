import { cn } from '@/lib/utils';

interface UserNameProps {
    firstName?: string;
    lastName?: string;
    displayName?: string;
    email?: string;
    className?: string;
    onClick?: () => void;
    showEmail?: boolean;
}

export function UserName({
    firstName,
    lastName,
    displayName,
    email,
    className,
    onClick,
    showEmail = false,
}: UserNameProps) {
    const getName = () => {
        if (firstName && lastName) {
            return `${firstName} ${lastName}`;
        }

        if (displayName) {
            return displayName;
        }

        if (email) {
            return email.split('@')[0];
        }

        return 'Unknown User';
    };

    return (
        <div className={cn('flex flex-col', className)}>
            <span
                className={cn(
                    'font-medium text-gray-900',
                    onClick && 'cursor-pointer hover:text-[#FF7C75] transition-colors'
                )}
                onClick={onClick}
            >
                {getName()}
            </span>
            {showEmail && email && (
                <span className='text-sm text-gray-500'>{email}</span>
            )}
        </div>
    );
} 