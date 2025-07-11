import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

const Hero = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // Redirect authenticated users to recipes page
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/recipes');
    }
  }, [isAuthenticated, navigate]);

  // Don't render anything if user is authenticated (will redirect)
  if (isAuthenticated) {
    return null;
  }

  return (
    <div
      id='hero'
      className='h-[91vh] overflow-hidden bg-gray-50'
    >
      <div className='container mx-auto px-8 pt-20 md:max-w-screen-md lg:max-w-screen-xl lg:pt-32'>
        <div className='grid grid-cols-1 items-center gap-8 lg:grid-cols-12'>
          <div className='col-span-6'>
            <h1 className='mb-5 text-center text-4xl font-semibold text-black lg:text-start lg:text-7xl'>
              Collaborative recipe making&nbsp;
              <span className='inline-block whitespace-nowrap text-[#FF7C75]'>redefined</span>
            </h1>
            <p className='mb-10 text-center font-normal text-black/55 lg:text-start lg:text-lg'>
              Create, modify, and share your favorite recipes
              <span className='hidden sm:inline'>
                <br />
              </span>
              and collaborate with others to discover new culinary adventures.
              <span className='hidden sm:inline'>
                <br />
              </span>
              Track the evolution of each recipe with a detailed history
              <span className='hidden sm:inline'>
                <br />
              </span>
              all in one intuitive app.
            </p>
            <div className='flex flex-row items-center justify-center gap-4 lg:justify-start'>
              <Button
                size='lg'
                onClick={() => navigate('/register')}
                className='bg-[#FF7C75] hover:bg-rose-600'
              >
                Get Started
              </Button>
              <Button
                variant='outline'
                size='lg'
                onClick={() => navigate('/login')}
                className='border-[#FF7C75] text-[#FF7C75] hover:bg-rose-50'
              >
                Login
              </Button>
            </div>
          </div>
          <div className='relative col-span-6 flex justify-center'>
            <img
              src='/hero.svg'
              alt='hero'
              width={1000}
              height={805}
              className='max-w-full h-auto'
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Hero;
