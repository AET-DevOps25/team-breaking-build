import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

export default function NotFoundPage() {
    return (
        <div
            id='hero'
            className='h-[91vh] overflow-hidden bg-gray-50'
        >
            <div className='container flex flex-col items-center justify-center gap-6 p-6 pt-20'>
                <img
                    src='/not-found.svg'
                    alt='Not Found'
                    width={650}
                    height={400}
                    className='max-w-full h-auto'
                />
                <div className='flex flex-col items-center justify-center'>
                    <h1 className='mt-6 text-3xl font-bold text-gray-800'>Page not found</h1>
                    <p className='mt-2 text-center text-gray-500'>Sorry, we couldn't find the page you're looking for.</p>
                </div>
                <Link to='/'>
                    <Button>Go to Homepage</Button>
                </Link>
            </div>
        </div>
    );
} 