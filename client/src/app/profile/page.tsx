'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

import { User, ChefHat, Plus, Heart, Clock, Users, LogOut } from 'lucide-react';
import Link from 'next/link';
import Image from 'next/image';

interface Recipe {
  id: string;
  title: string;
  description: string;
  imageUrl?: string;
  cookingTime: number;
  servings: number;
  likes: number;
  createdAt: string;
}

export default function ProfilePage() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const router = useRouter();
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [isLoadingRecipes, setIsLoadingRecipes] = useState(true);
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
      return;
    }

    if (isAuthenticated && user) {
      const fetchUserRecipes = async () => {
        try {
          // Import the recipe service dynamically to avoid circular dependencies
          const { getUserRecipes } = await import('@/lib/services/recipeService');
          const userRecipes = await getUserRecipes(user.id);
          // Transform the API response to match the local Recipe interface
          const transformedRecipes = userRecipes.map((recipe) => ({
            id: recipe.id,
            title: recipe.title,
            description: recipe.description,
            imageUrl: recipe.thumbnail?.url,
            cookingTime: 30, // Default value since API might not have this
            servings: recipe.servingSize,
            likes: 0, // Default value since API might not have this
            createdAt: recipe.createdAt,
          }));
          setRecipes(transformedRecipes);
        } catch {
          setRecipes([]); // Empty array on error
        } finally {
          setIsLoadingRecipes(false);
        }
      };

      fetchUserRecipes();
    }
  }, [isAuthenticated, isLoading, router, user]);

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

        {isLoadingRecipes ? (
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
          <div className='grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3'>
            {recipes.map((recipe) => (
              <Card
                key={recipe.id}
                className='overflow-hidden shadow-lg transition-shadow duration-300 hover:shadow-xl'
              >
                <div className='flex h-48 items-center justify-center bg-gradient-to-br from-rose-100 to-rose-200'>
                  {recipe.imageUrl ? (
                    <Image
                      src={recipe.imageUrl}
                      alt={recipe.title}
                      width={400}
                      height={200}
                      className='size-full object-cover'
                    />
                  ) : (
                    <ChefHat className='size-16 text-[#FF7C75]' />
                  )}
                </div>
                <CardContent className='p-4'>
                  <h3 className='mb-2 text-lg font-semibold text-gray-900'>{recipe.title}</h3>
                  <p className='mb-3 line-clamp-2 text-sm text-gray-600'>{recipe.description}</p>
                  <div className='flex items-center justify-between text-sm text-gray-500'>
                    <div className='flex items-center space-x-4'>
                      <div className='flex items-center'>
                        <Clock className='mr-1 size-4' />
                        {recipe.cookingTime}min
                      </div>
                      <div className='flex items-center'>
                        <Users className='mr-1 size-4' />
                        {recipe.servings}
                      </div>
                    </div>
                    <div className='flex items-center'>
                      <Heart className='mr-1 size-4 text-[#FF7C75]' />
                      {recipe.likes}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
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
              <Link href='/recipes/create'>
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
              <p className='mt-2 text-sm text-gray-600'>
                Are you sure you want to logout? You will need to log in again to access your account.
              </p>
            </div>
            <div className='mt-6 flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2'>
              <Button
                variant='outline'
                onClick={() => setShowLogoutDialog(false)}
                className='border-gray-300 text-gray-700 hover:bg-gray-50 hover:text-gray-900'
              >
                Cancel
              </Button>
              <Button
                onClick={() => {
                  setShowLogoutDialog(false);
                  logout();
                }}
                className='bg-[#FF7C75] text-white hover:bg-rose-600'
              >
                <LogOut className='mr-2 size-4' />
                Logout
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
