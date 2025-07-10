import type { Metadata } from 'next';
import '@/styles/globals.css';
import { inter } from '@/lib/fonts';
import NavBar from '@/components/navigation/NavBar';
import { Toaster } from 'sonner';
import { AuthProvider } from '@/contexts/AuthContext';
import { Chatbot } from '@/components/chatbot/Chatbot';

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
        <AuthProvider>
          <NavBar />
          <main>
            <div>{children}</div>
          </main>
          <Toaster />
          <Chatbot />
        </AuthProvider>
      </body>
    </html>
  );
}
