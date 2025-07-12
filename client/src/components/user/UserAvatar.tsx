import { cn } from '@/lib/utils';

interface UserAvatarProps {
    firstName?: string;
    lastName?: string;
    displayName?: string;
    email?: string;
    size?: 'sm' | 'md' | 'lg' | 'xl';
    className?: string;
    onClick?: () => void;
}

export function UserAvatar({
    firstName,
    lastName,
    displayName,
    email,
    size = 'md',
    className,
    onClick,
}: UserAvatarProps) {
    const sizeClasses = {
        sm: 'size-6 text-xs',
        md: 'size-8 text-sm',
        lg: 'size-10 text-base',
        xl: 'size-12 text-lg',
    };

    // Generate initials from user name
    const getInitials = () => {
        if (firstName && lastName) {
            return `${firstName.charAt(0).toUpperCase()}${lastName.charAt(0).toUpperCase()}`;
        }

        if (displayName) {
            const parts = displayName.split(' ').filter(part => part.length > 0);
            if (parts.length >= 2) {
                return `${parts[0].charAt(0).toUpperCase()}${parts[1].charAt(0).toUpperCase()}`;
            }
            return parts[0].charAt(0).toUpperCase();
        }

        if (email) {
            return email.charAt(0).toUpperCase();
        }

        return 'U';
    };

    return (
        <div
            className={cn(
                'flex items-center justify-center rounded-full bg-[#FF7C75] text-white font-semibold shrink-0',
                sizeClasses[size],
                onClick && 'cursor-pointer hover:bg-[#FF7C75]/80 transition-colors',
                className
            )}
            onClick={onClick}
        >
            {getInitials()}
        </div>
    );
} 