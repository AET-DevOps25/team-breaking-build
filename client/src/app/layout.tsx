import type { Metadata } from 'next';
import '@/styles/globals.css';
import { inter } from '@/lib/fonts';
import NavBar from '@/components/navigation/NavBar';

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
          <div>{children}</div>
        </main>
      </body>
    </html>
  );
}
