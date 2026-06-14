import type { Metadata } from 'next';
import './globals.css';
import Navbar from '@/components/Navbar';

export const metadata: Metadata = {
  title: 'AeroLink — AI Flight Assistant',
  description: 'Search, book, and manage flights with the power of multi-agent AI',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-space text-white">
        <Navbar />
        <main>{children}</main>
      </body>
    </html>
  );
}
