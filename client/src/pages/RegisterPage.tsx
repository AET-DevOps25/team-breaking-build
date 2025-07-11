import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { UtensilsCrossed } from 'lucide-react';
import { z } from 'zod';

const emailSchema = z.string().email('Please enter a valid email address');

export default function RegisterPage() {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { register } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        try {
            // Validate email
            emailSchema.parse(email);
        } catch {
            setError('Please enter a valid email address');
            return;
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        setIsLoading(true);

        try {
            await register(email, password, firstName, lastName);
            navigate('/');
        } catch {
            setError('Registration failed. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className='flex min-h-[calc(100vh-73px)] items-center justify-center bg-gradient-to-br from-rose-50 to-rose-100'>
            <div className='w-full max-w-md p-6'>
                <div className='mb-8 text-center'>
                    <UtensilsCrossed className='mx-auto mb-4 size-16 text-[#FF7C75]' />
                    <h1 className='text-3xl font-bold text-gray-900'>Join Recipefy</h1>
                    <p className='mt-2 text-gray-600'>Start your culinary journey today</p>
                </div>
                <Card className='w-full border-0 shadow-lg'>
                    <CardHeader>
                        <CardTitle className='text-center text-2xl'>Create Account</CardTitle>
                        <CardDescription className='text-center'>Share your recipes with the world</CardDescription>
                    </CardHeader>
                    <form onSubmit={handleSubmit}>
                        <CardContent className='space-y-4'>
                            {error && (
                                <Alert variant='destructive'>
                                    <AlertDescription>{error}</AlertDescription>
                                </Alert>
                            )}
                            <div className='grid grid-cols-2 gap-4'>
                                <div className='space-y-2'>
                                    <Label htmlFor='firstName'>First Name</Label>
                                    <Input
                                        id='firstName'
                                        type='text'
                                        value={firstName}
                                        onChange={(e) => setFirstName(e.target.value)}
                                        placeholder='Gordon'
                                        required
                                        className='border-2 focus:border-[#FF7C75] focus:ring-[#FF7C75]'
                                    />
                                </div>
                                <div className='space-y-2'>
                                    <Label htmlFor='lastName'>Last Name</Label>
                                    <Input
                                        id='lastName'
                                        type='text'
                                        value={lastName}
                                        onChange={(e) => setLastName(e.target.value)}
                                        placeholder='Ramsay'
                                        required
                                        className='border-2 focus:border-[#FF7C75] focus:ring-[#FF7C75]'
                                    />
                                </div>
                            </div>
                            <div className='space-y-2'>
                                <Label htmlFor='email'>Email</Label>
                                <Input
                                    id='email'
                                    type='email'
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder='chef@recipefy.com'
                                    required
                                    className='border-2 focus:border-[#FF7C75] focus:ring-[#FF7C75]'
                                />
                            </div>
                            <div className='space-y-2'>
                                <Label htmlFor='password'>Password</Label>
                                <Input
                                    id='password'
                                    type='password'
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder='Secret recipe ingredients'
                                    required
                                    className='border-2 focus:border-[#FF7C75] focus:ring-[#FF7C75]'
                                />
                            </div>
                            <div className='space-y-2'>
                                <Label htmlFor='confirmPassword'>Confirm Password</Label>
                                <Input
                                    id='confirmPassword'
                                    type='password'
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    placeholder='Double-check your secret recipe'
                                    required
                                    className='border-2 focus:border-[#FF7C75] focus:ring-[#FF7C75]'
                                />
                            </div>
                        </CardContent>
                        <CardFooter className='flex flex-col space-y-4'>
                            <Button
                                type='submit'
                                className='w-full rounded-md bg-[#FF7C75] px-4 py-2 font-bold text-white transition-colors hover:bg-rose-600'
                                disabled={isLoading}
                            >
                                {isLoading ? 'Preparing your account...' : 'Create Account'}
                            </Button>
                            <p className='text-center text-sm text-gray-600'>
                                Already have an account?{' '}
                                <Link
                                    to='/login'
                                    className='font-semibold text-[#FF7C75] hover:underline'
                                >
                                    Back to the kitchen
                                </Link>
                            </p>
                        </CardFooter>
                    </form>
                </Card>
            </div>
        </div>
    );
} 