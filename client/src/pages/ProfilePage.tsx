import { useEffect, useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { InfiniteScroll } from '@/components/ui/infinite-scroll';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { getUserRecipes } from '@/lib/services/recipeService';
import { RecipeCard } from '@/components/recipe/recipe-card';
import { Recipe } from '@/lib/types/recipe';

import { User, ChefHat, Plus, LogOut } from 'lucide-react';

export default function ProfilePage() {
    const { user, isAuthenticated, isLoading, logout } = useAuth();
    const navigate = useNavigate();
    const [showLogoutDialog, setShowLogoutDialog] = useState(false);

    // Create a fetch function for user recipes
    const fetchUserRecipes = useCallback(
        async (page: number) => {
            if (!user?.id) return [];

            const userRecipes = await getUserRecipes(user.id, page, 10);
            return userRecipes;
        },
        [user?.id],
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

    // Reset infinite scroll when user changes
    useEffect(() => {
        if (user?.id) {
            reset();
        }
    }, [user?.id, reset]);

    useEffect(() => {
        if (!isLoading && !isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, isLoading, navigate]);

    if (isLoading) {
        return (
            <div className='flex min-h-screen items-center justify-center'>
                <div className='size-32 animate-spin rounded-full border-b-2 border-[#FF7C75]'></div>
            </div>
        );
    }

    if (!isAuthenticated) {
        return null;
    }

    // Don't render infinite scroll until user is loaded
    if (!user?.id) {
        return (
            <div className='flex min-h-screen items-center justify-center'>
                <div className='size-32 animate-spin rounded-full border-b-2 border-[#FF7C75]'></div>
            </div>
        );
    }

    const handleLogout = () => {
        logout();
        setShowLogoutDialog(false);
        navigate('/');
    };

    return (
        <div className='min-h-screen bg-white pt-20'>
            <div className='mx-auto max-w-4xl px-4 py-8'>
                {/* Profile Details Section */}
                <div className='mb-6'>
                    <h2 className='mb-4 flex items-center text-2xl font-bold text-gray-900'>
                        <User className='mr-2 size-6 text-[#FF7C75]' />
                        Profile Details
                    </h2>
                </div>

                <Card className='mx-4 mb-8 border-0 shadow-lg'>
                    <CardContent className='p-8'>
                        <div className='flex items-center space-x-8'>
                            <div className='flex size-24 shrink-0 items-center justify-center rounded-full bg-[#FF7C75] shadow-lg'>
                                <span className='text-2xl font-bold text-white'>
                                    {user?.firstName && user?.lastName
                                        ? `${user.firstName.charAt(0).toUpperCase()}${user.lastName.charAt(0).toUpperCase()}`
                                        : user?.email?.charAt(0).toUpperCase() || 'C'}
                                </span>
                            </div>
                            <div className='flex-1'>
                                <h1 className='mb-2 text-3xl font-bold text-gray-900'>
                                    {user?.firstName && user?.lastName
                                        ? `${user.firstName} ${user.lastName}`
                                        : user?.email?.split('@')[0] || 'Chef'}
                                </h1>
                                <p className='mb-4 text-lg text-gray-600'>{user?.email}</p>
                            </div>
                            <div className='shrink-0'>
                                <Button
                                    onClick={() => setShowLogoutDialog(true)}
                                    className='rounded-lg bg-[#FF7C75] px-6 py-3 font-semibold text-white shadow-md transition-colors duration-200 hover:bg-rose-600 hover:shadow-lg'
                                >
                                    <LogOut className='mr-2 size-4' />
                                    Logout
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Recipes Section */}
                <div className='mb-6'>
                    <h2 className='mb-4 flex items-center text-2xl font-bold text-gray-900'>
                        <ChefHat className='mr-2 size-6 text-[#FF7C75]' />
                        My Recipes
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
                            {recipes.map((recipe) => (
                                <RecipeCard
                                    key={recipe.id}
                                    recipe={recipe}
                                    onClick={() => navigate(`/recipes/${recipe.id}`)}
                                />
                            ))}
                        </div>
                    </InfiniteScroll>
                ) : (
                    /* Empty State - Call to Action */
                    <Card className='border-0 py-12 text-center shadow-lg'>
                        <CardContent>
                            <div className='mx-auto mb-6 flex size-24 items-center justify-center rounded-full bg-rose-100'>
                                <ChefHat className='size-12 text-[#FF7C75]' />
                            </div>
                            <h3 className='mb-4 text-2xl font-bold text-gray-900'>No recipes yet!</h3>
                            <p className='mx-auto mb-8 max-w-md text-gray-600'>
                                Start your culinary journey by creating your first recipe. Share your favorite dishes with the world!
                            </p>
                            <Link to='/recipes/create'>
                                <Button className='mx-auto flex items-center rounded-lg bg-[#FF7C75] px-6 py-3 font-semibold text-white transition-colors duration-200 hover:bg-rose-600'>
                                    <Plus className='mr-2 size-5' />
                                    Create Your First Recipe
                                </Button>
                            </Link>
                        </CardContent>
                    </Card>
                )}
            </div>

            {/* Logout Confirmation Dialog */}
            {showLogoutDialog && (
                <div className='fixed inset-0 z-50 flex items-center justify-center'>
                    <div
                        className='fixed inset-0 bg-black/50 backdrop-blur-sm'
                        onClick={() => setShowLogoutDialog(false)}
                    />
                    <div className='relative z-50 m-4 w-full max-w-lg rounded-lg bg-white p-6 shadow-lg'>
                        <div className='flex flex-col space-y-1.5 text-center sm:text-left'>
                            <h2 className='text-lg font-semibold leading-none tracking-tight'>Confirm Logout</h2>
                            <p className='text-sm text-gray-600'>
                                Are you sure you want to log out? You'll need to sign in again to access your account.
                            </p>
                        </div>
                        <div className='flex flex-col-reverse gap-2 pt-6 sm:flex-row sm:justify-end'>
                            <Button
                                variant='outline'
                                onClick={() => setShowLogoutDialog(false)}
                                className='border-2 border-gray-300 px-4 py-2'
                            >
                                Cancel
                            </Button>
                            <Button
                                onClick={handleLogout}
                                className='bg-[#FF7C75] px-4 py-2 text-white hover:bg-rose-600'
                            >
                                Logout
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
} 