'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import clsx from 'clsx';

const links = [
  { name: 'Home', href: '/' },
  { name: 'Recipes', href: '/recipes' },
  { name: 'Create', href: '/create' },
  { name: 'Login', href: '/login' },
];

export default function NavLinks() {
  const pathname = usePathname();

  return (
    <div className='flex gap-3'>
      {links.map((link) => {
        return (
          <Link
            key={link.name}
            href={link.href}
            className={clsx(
              'flex h-[48px] flex-none grow items-center justify-start gap-2 rounded-md p-2 px-3 text-sm font-bold text-[#FF7C75] hover:bg-rose-100 hover:text-rose-600',
              {
                'underline underline-offset-5 decoration-3': pathname === link.href,
              },
            )}
          >
            <p>{link.name}</p>
          </Link>
        );
      })}
    </div>
  );
}
