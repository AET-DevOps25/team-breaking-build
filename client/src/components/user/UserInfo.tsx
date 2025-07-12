import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserAvatar } from './UserAvatar';
import { UserName } from './UserName';
import { getUserDisplayInfo } from '@/lib/services/userService';
import { UserDisplayInfo } from '@/lib/types/user';
import { cn } from '@/lib/utils';

interface UserInfoProps {
    userId: string;
    size?: 'sm' | 'md' | 'lg' | 'xl';
    layout?: 'horizontal' | 'vertical';
    showEmail?: boolean;
    clickable?: boolean;
    className?: string;
}

export function UserInfo({
    userId,
    size = 'md',
    layout = 'horizontal',
    showEmail = false,
    clickable = true,
    className,
}: UserInfoProps) {
    const [userInfo, setUserInfo] = useState<UserDisplayInfo | null>(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                setLoading(true);
                const info = await getUserDisplayInfo(userId);
                setUserInfo(info);
            } catch (error) {
                console.error('Failed to fetch user info:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchUserInfo();
    }, [userId]);

    const handleClick = () => {
        if (clickable && userInfo) {
            navigate(`/users/${userInfo.id}`);
        }
    };

    if (loading) {
        return (
            <div className={cn('animate-pulse flex items-center gap-2', className)}>
                <div className='rounded-full bg-gray-200 size-8'></div>
                <div className='h-4 w-20 bg-gray-200 rounded'></div>
            </div>
        );
    }

    if (!userInfo) {
        return (
            <div className={cn('flex items-center gap-2 text-gray-500', className)}>
                <UserAvatar size={size} />
                <span className='text-sm'>Unknown User</span>
            </div>
        );
    }

    return (
        <div
            className={cn(
                'flex items-center gap-2',
                layout === 'vertical' && 'flex-col gap-1',
                className
            )}
        >
            <UserAvatar
                firstName={userInfo.firstName}
                lastName={userInfo.lastName}
                displayName={userInfo.displayName}
                email={userInfo.email}
                size={size}
                onClick={clickable ? handleClick : undefined}
            />
            <UserName
                firstName={userInfo.firstName}
                lastName={userInfo.lastName}
                displayName={userInfo.displayName}
                email={userInfo.email}
                showEmail={showEmail}
                onClick={clickable ? handleClick : undefined}
            />
        </div>
    );
} 