'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Plane, Map, TrendingDown, MessageSquare, Briefcase } from 'lucide-react';

const links = [
  { href: '/',        label: 'Search',    icon: Plane },
  { href: '/manage',  label: 'My Trips',  icon: Briefcase },
  { href: '/tracker', label: 'Price Tracker', icon: TrendingDown },
  { href: '/chat',    label: 'AI Assistant',  icon: MessageSquare },
];

export default function Navbar() {
  const path = usePathname();
  return (
    <nav className="fixed top-0 inset-x-0 z-50 glass-dark border-b border-white/5">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2 group">
          <div className="w-8 h-8 rounded-lg btn-primary flex items-center justify-center shadow-lg">
            <Plane className="w-4 h-4 text-white rotate-45" />
          </div>
          <span className="text-lg font-bold gradient-text">AeroLink</span>
        </Link>

        {/* Nav links */}
        <div className="hidden md:flex items-center gap-1">
          {links.map(({ href, label, icon: Icon }) => {
            const active = path === href;
            return (
              <Link
                key={href}
                href={href}
                className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                  active
                    ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-gray-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Icon className="w-4 h-4" />
                {label}
              </Link>
            );
          })}
        </div>

        {/* Mobile menu simplified */}
        <div className="flex md:hidden items-center gap-2">
          {links.map(({ href, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className={`p-2 rounded-lg transition-colors ${
                path === href ? 'text-indigo-400 bg-indigo-500/20' : 'text-gray-400 hover:text-white'
              }`}
            >
              <Icon className="w-5 h-5" />
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
