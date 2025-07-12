import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { InfiniteScroll } from '@/components/ui/infinite-scroll';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { getUserRecipes } from '@/lib/services/recipeService';
import { getUserDisplayInfo } from '@/lib/services/userService';
import { RecipeCard } from '@/components/recipe/recipe-card';
import { Recipe } from '@/lib/types/recipe';
import { UserDisplayInfo } from '@/lib/types/user';
import { UserAvatar } from '@/components/user/UserAvatar';
import { ArrowLeft, ChefHat, User } from 'lucide-react';

export default function UserProfilePage() {
    const params = useParams();
    const navigate = useNavigate();
    const { user: currentUser } = useAuth();
    const [userInfo, setUserInfo] = useState<UserDisplayInfo | null>(null);
    const [isLoadingUser, setIsLoadingUser] = useState(true);
    const userId = params.userId as string;

    // Create a fetch function for user recipes
    const fetchUserRecipes = useCallback(
        async (page: number) => {
            if (!userId) return [];

            const userRecipes = await getUserRecipes(userId, page, 10);
            return userRecipes;
        },
        [userId],
    );

    const {
        data: recipes,
        loading: isLoadingRecipes,
        hasMore,
        error,
        loadMore,
        reset,
    } = useInfiniteScroll<Recipe>({
        fetchData: fetchUserRecipes,
        pageSize: 10,
        initialPage: 0,
    });

    // Fetch user information
    useEffect(() => {
        const fetchUserInfo = async () => {
            if (!userId) return;

            try {
                setIsLoadingUser(true);
                const info = await getUserDisplayInfo(userId);
                setUserInfo(info);
            } catch (error) {
                console.error('Failed to fetch user info:', error);
            } finally {
                setIsLoadingUser(false);
            }
        };

        fetchUserInfo();
    }, [userId]);

    // Reset infinite scroll when userId changes
    useEffect(() => {
        if (userId) {
            reset();
        }
    }, [userId, reset]);

    // Redirect to own profile if viewing own user ID
    useEffect(() => {
        if (currentUser && userId === currentUser.id) {
            navigate('/profile');
        }
    }, [currentUser, userId, navigate]);

    const handleBackClick = () => {
        navigate(-1);
    };

    if (isLoadingUser) {
        return (
            <div className='flex min-h-screen items-center justify-center'>
                <div className='size-32 animate-spin rounded-full border-b-2 border-[#FF7C75]'></div>
            </div>
        );
    }

    if (!userInfo) {
        return (
            <div className='min-h-screen bg-white pt-20'>
                <div className='mx-auto max-w-4xl px-4 py-8'>
                    <div className='text-center'>
                        <h1 className='mb-4 text-2xl font-bold text-gray-900'>User Not Found</h1>
                        <p className='mb-8 text-gray-600'>
                            The user you&apos;re looking for doesn&apos;t exist or has been removed.
                        </p>
                        <Button onClick={handleBackClick} className='bg-[#FF7C75] hover:bg-[#FF7C75]/90'>
                            <ArrowLeft className='mr-2 size-4' />
                            Go Back
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className='min-h-screen bg-white pt-20'>
            <div className='mx-auto max-w-4xl px-4 py-8'>
                {/* Back Button */}
                <Button
                    onClick={handleBackClick}
                    variant='outline'
                    size='sm'
                    className='mb-6 border-[#FF7C75] text-[#FF7C75] hover:bg-[#FF7C75] hover:text-white'
                >
                    <ArrowLeft className='mr-2 size-4' />
                    Back
                </Button>

                {/* Profile Details Section */}
                <div className='mb-6'>
                    <h2 className='mb-4 flex items-center text-2xl font-bold text-gray-900'>
                        <User className='mr-2 size-6 text-[#FF7C75]' />
                        Profile
                    </h2>
                </div>

                <Card className='mx-4 mb-8 border-0 shadow-lg'>
                    <CardContent className='p-8'>
                        <div className='flex items-center space-x-8'>
                            <UserAvatar
                                firstName={userInfo.firstName}
                                lastName={userInfo.lastName}
                                displayName={userInfo.displayName}
                                email={userInfo.email}
                                size='xl'
                                className='size-24 text-2xl'
                            />
                            <div className='flex-1'>
                                <h1 className='mb-2 text-3xl font-bold text-gray-900'>
                                    {userInfo.displayName}
                                </h1>
                                <p className='mb-4 text-lg text-gray-600'>{userInfo.email}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Recipes Section */}
                <div className='mb-6'>
                    <h2 className='mb-4 flex items-center text-2xl font-bold text-gray-900'>
                        <ChefHat className='mr-2 size-6 text-[#FF7C75]' />
                        {userInfo.displayName}&apos;s Recipes
                    </h2>
                </div>

                {error && (
                    <div className='mb-6 rounded-lg bg-red-50 p-4 text-red-700'>
                        <p>Error loading recipes: {error}</p>
                    </div>
                )}

                {isLoadingRecipes && recipes.length === 0 ? (
                    <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
                        {[...Array(6)].map((_, i) => (
                            <Card
                                key={i}
                                className='animate-pulse'
                            >
                                <div className='h-48 rounded-t-lg bg-gray-200'></div>
                                <CardContent className='p-4'>
                                    <div className='mb-2 h-4 rounded bg-gray-200'></div>
                                    <div className='h-3 w-3/4 rounded bg-gray-200'></div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                ) : recipes.length > 0 ? (
                    <InfiniteScroll
                        onLoadMore={loadMore}
                        hasMore={hasMore}
                        loading={isLoadingRecipes}
                    >
                        <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
                            {recipes.slice().reverse().map((recipe) => (
                                <RecipeCard
                                    key={recipe.id}
                                    recipe={recipe}
                                    onClick={() => navigate(`/recipes/${recipe.id}`)}
                                />
                            ))}
                        </div>
                    </InfiniteScroll>
                ) : (
                    /* Empty State */
                    <Card className='border-0 py-12 text-center shadow-lg'>
                        <CardContent>
                            <div className='mx-auto mb-6 flex size-24 items-center justify-center rounded-full bg-rose-100'>
                                <ChefHat className='size-12 text-[#FF7C75]' />
                            </div>
                            <h3 className='mb-4 text-2xl font-bold text-gray-900'>No recipes yet!</h3>
                            <p className='mx-auto mb-8 max-w-md text-gray-600'>
                                {userInfo.displayName} hasn&apos;t shared any recipes yet. Check back later for delicious creations!
                            </p>
                        </CardContent>
                    </Card>
                )}
            </div>
        </div>
    );
} 