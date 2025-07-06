'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import clsx from 'clsx';
import { useAuth } from '@/contexts/AuthContext';

const publicLinks = [
  { name: 'Home', href: '/' },
  { name: 'Recipes', href: '/recipes' },
];

const authLinks = [
  { name: 'Recipes', href: '/recipes' },
  { name: 'Create', href: '/recipes/create' },
];

export default function NavLinks() {
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();

  const links = [
    ...(isAuthenticated ? authLinks : publicLinks),
    ...(isAuthenticated ? [] : [{ name: 'Login', href: '/login' }]),
  ];

  return (
    <div className='flex items-center gap-3'>
      {links.map((link) => (
        <Link
          key={link.name}
          href={link.href}
          className={clsx(
            'flex h-[48px] flex-none grow items-center justify-start gap-2 rounded-md p-2 px-3 text-sm font-bold text-[#FF7C75] hover:bg-rose-100 hover:text-rose-600',
            {
              'underline underline-offset-4 decoration-4': pathname === link.href,
            },
          )}
        >
          <p>{link.name}</p>
        </Link>
      ))}
      {isAuthenticated && (
        <Link
          href='/profile'
          className={clsx(
            'flex h-[48px] flex-none grow items-center justify-start gap-2 rounded-md p-2 px-3 text-sm font-bold text-[#FF7C75] hover:bg-rose-100 hover:text-rose-600',
            {
              'underline underline-offset-4 decoration-4': pathname === '/profile',
            },
          )}
        >
          Profile
        </Link>
      )}
    </div>
  );
}
