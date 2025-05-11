import type { Metadata } from "next";
import "./globals.css";
import { inter } from '@/app/ui/fonts';
import NavBar from "@/app/components/navigation/NavBar";

export const metadata: Metadata = {
  title: "Recipefy",
  description: "Collaborative recipe making redefined.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${inter.className} antialiased bg-[#fafafa]`}
      >
      <NavBar />
      <main className="p-4">{children}</main>
      </body>
    </html>
  );
}
