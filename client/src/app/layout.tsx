import type { Metadata } from 'next';
import './globals.css';
import { inter } from '@/app/ui/fonts';
import NavBar from '@/app/components/navigation/NavBar';

export const metadata: Metadata = {
  title: 'Recipefy',
  description: 'Collaborative recipe making redefined.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang='en'>
      <body className={`${inter.className} bg-[#fafafa] antialiased`}>
        <NavBar />
        <main>
          <div className='min-w-[1200px] p-16'>{children}</div>
        </main>
      </body>
    </html>
  );
}
