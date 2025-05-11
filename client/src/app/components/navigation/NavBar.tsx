import Image from 'next/image';
import Link from 'next/link';
import NavLinks from './NavLinks';

export default function NavBar() {
    return (
        <div className='sticky top-0 z-20 shadow-md'>
            <div className='flex justify-between bg-white px-8 py-4'>
                <Link href='/'>
                    <Image
                        src='/logo-color.svg'
                        alt='Recipefy Logo'
                        width={160}
                        height={40}
                    />
                </Link>
                <NavLinks/>
            </div>
        </div>
    );
}
