'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Plane, ArrowLeftRight, Calendar, Users, ChevronDown, Search, Star, Shield, Zap } from 'lucide-react';

const DESTINATIONS = [
  { city: 'Tokyo',   code: 'NRT', emoji: '🗼', tagline: 'Cherry blossoms & neon lights', gradient: 'from-pink-600 to-rose-800' },
  { city: 'London',  code: 'LHR', emoji: '🎡', tagline: 'Historic charm meets modern flair', gradient: 'from-sky-600 to-blue-800' },
  { city: 'Dubai',   code: 'DXB', emoji: '🏙️', tagline: 'Where luxury meets the desert', gradient: 'from-amber-600 to-orange-800' },
  { city: 'Paris',   code: 'CDG', emoji: '🗼', tagline: 'The city of love & haute cuisine', gradient: 'from-indigo-600 to-purple-800' },
];

const FEATURES = [
  { icon: Zap,    title: 'AI-Powered Search',    desc: 'Our multi-agent AI finds the best routes across hundreds of airlines instantly.' },
  { icon: Shield, title: 'Secure Booking',        desc: 'Bank-level encryption protects every transaction from search to boarding pass.' },
  { icon: Star,   title: 'Real-Time Prices',      desc: 'Price alerts notify you the moment fares drop to your target price.' },
];

const CLASSES = ['ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST'];

export default function HomePage() {
  const router = useRouter();
  const [tripType, setTripType] = useState<'one-way' | 'round-trip'>('one-way');
  const [from, setFrom]         = useState('');
  const [to, setTo]             = useState('');
  const [date, setDate]         = useState('');
  const [returnDate, setReturn] = useState('');
  const [adults, setAdults]     = useState(1);
  const [cls, setCls]           = useState('ECONOMY');
  const [showCls, setShowCls]   = useState(false);

  function swap() { setFrom(to); setTo(from); }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!from || !to || !date) return;
    const p = new URLSearchParams({
      origin: from.toUpperCase(), destination: to.toUpperCase(),
      departure_date: date, adults: String(adults), travel_class: cls,
      ...(tripType === 'round-trip' && returnDate ? { return_date: returnDate } : {}),
    });
    router.push(`/search?${p.toString()}`);
  }

  return (
    <div className="min-h-screen">
      {/* ── Hero ─────────────────────────────────────────────────────────────── */}
      <section className="relative min-h-screen aurora-bg flex flex-col items-center justify-center px-4 pt-16 overflow-hidden">
        {/* Radial glow blobs */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full bg-indigo-600/15 blur-3xl pointer-events-none" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full bg-purple-600/15 blur-3xl pointer-events-none" />

        {/* Floating plane */}
        <div className="absolute top-28 right-16 md:right-32 opacity-20 animate-float pointer-events-none">
          <Plane className="w-32 h-32 text-indigo-400" />
        </div>

        {/* Headline */}
        <div className="text-center mb-10 animate-slide-up">
          <div className="inline-flex items-center gap-2 glass rounded-full px-4 py-2 mb-6 border border-indigo-500/20">
            <Zap className="w-3.5 h-3.5 text-amber-400" />
            <span className="text-xs text-gray-300 font-medium">Powered by multi-agent AI</span>
          </div>
          <h1 className="text-5xl md:text-7xl font-black text-white leading-none tracking-tight">
            Discover Your
            <br />
            <span className="gradient-text">World</span>
          </h1>
          <p className="mt-4 text-gray-400 text-lg max-w-md mx-auto">
            Search, book, and manage flights with an AI that thinks ahead for you.
          </p>
        </div>

        {/* Search card */}
        <div className="w-full max-w-3xl glass rounded-3xl p-6 border border-white/10 shadow-2xl animate-fade-in">
          {/* Trip type toggle */}
          <div className="flex gap-2 mb-5">
            {(['one-way', 'round-trip'] as const).map(t => (
              <button
                key={t}
                onClick={() => setTripType(t)}
                className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                  tripType === t
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                {t === 'one-way' ? 'One Way' : 'Round Trip'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSearch} className="space-y-4">
            {/* From / To */}
            <div className="flex items-center gap-2">
              <div className="flex-1">
                <label className="text-xs text-gray-500 mb-1 block">From</label>
                <input
                  value={from} onChange={e => setFrom(e.target.value)}
                  placeholder="JFK — New York"
                  className="input-dark w-full rounded-xl px-4 py-3 text-sm uppercase placeholder:normal-case"
                  required
                />
              </div>
              <button type="button" onClick={swap}
                className="mt-5 p-2.5 rounded-xl glass hover:bg-white/10 transition-colors text-gray-400 hover:text-white">
                <ArrowLeftRight className="w-4 h-4" />
              </button>
              <div className="flex-1">
                <label className="text-xs text-gray-500 mb-1 block">To</label>
                <input
                  value={to} onChange={e => setTo(e.target.value)}
                  placeholder="LHR — London"
                  className="input-dark w-full rounded-xl px-4 py-3 text-sm uppercase placeholder:normal-case"
                  required
                />
              </div>
            </div>

            {/* Dates + passengers + class */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div className={tripType === 'round-trip' ? '' : 'md:col-span-2'}>
                <label className="text-xs text-gray-500 mb-1 flex items-center gap-1">
                  <Calendar className="w-3 h-3" /> Depart
                </label>
                <input type="date" value={date} onChange={e => setDate(e.target.value)}
                  className="input-dark w-full rounded-xl px-3 py-3 text-sm" required />
              </div>

              {tripType === 'round-trip' && (
                <div>
                  <label className="text-xs text-gray-500 mb-1 flex items-center gap-1">
                    <Calendar className="w-3 h-3" /> Return
                  </label>
                  <input type="date" value={returnDate} onChange={e => setReturn(e.target.value)}
                    className="input-dark w-full rounded-xl px-3 py-3 text-sm" />
                </div>
              )}

              <div>
                <label className="text-xs text-gray-500 mb-1 flex items-center gap-1">
                  <Users className="w-3 h-3" /> Passengers
                </label>
                <div className="flex items-center gap-2 input-dark rounded-xl px-3 py-2.5">
                  <button type="button" onClick={() => setAdults(Math.max(1, adults - 1))}
                    className="text-gray-400 hover:text-white font-bold text-lg leading-none">−</button>
                  <span className="flex-1 text-center text-sm font-semibold">{adults}</span>
                  <button type="button" onClick={() => setAdults(Math.min(9, adults + 1))}
                    className="text-gray-400 hover:text-white font-bold text-lg leading-none">+</button>
                </div>
              </div>

              <div className="relative">
                <label className="text-xs text-gray-500 mb-1 block">Class</label>
                <button type="button" onClick={() => setShowCls(!showCls)}
                  className="input-dark w-full rounded-xl px-3 py-3 text-sm flex items-center justify-between">
                  <span>{cls.replace('_', ' ')}</span>
                  <ChevronDown className={`w-4 h-4 transition-transform ${showCls ? 'rotate-180' : ''}`} />
                </button>
                {showCls && (
                  <div className="absolute top-full mt-1 left-0 right-0 glass-dark rounded-xl border border-white/10 overflow-hidden z-20">
                    {CLASSES.map(c => (
                      <button key={c} type="button"
                        onClick={() => { setCls(c); setShowCls(false); }}
                        className={`w-full px-4 py-2.5 text-sm text-left hover:bg-indigo-600/20 transition-colors ${c === cls ? 'text-indigo-300' : 'text-gray-300'}`}>
                        {c.replace('_', ' ')}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <button type="submit"
              className="btn-primary w-full text-white font-bold py-4 rounded-2xl flex items-center justify-center gap-2 text-base">
              <span className="relative z-10 flex items-center gap-2">
                <Search className="w-5 h-5" /> Search Flights
              </span>
            </button>
          </form>
        </div>

        {/* Scroll hint */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1 animate-bounce opacity-40">
          <ChevronDown className="w-5 h-5 text-white" />
        </div>
      </section>

      {/* ── Popular destinations ─────────────────────────────────────────────── */}
      <section className="py-20 px-4 bg-navy">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-3">
              Popular <span className="gradient-text">Destinations</span>
            </h2>
            <p className="text-gray-400">Handpicked routes with the best fares right now</p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {DESTINATIONS.map(dest => (
              <button key={dest.code}
                onClick={() => { setTo(dest.code); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
                className={`relative bg-gradient-to-br ${dest.gradient} rounded-2xl p-6 text-left card-hover border border-white/10 overflow-hidden group`}>
                <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-white/5 -translate-y-6 translate-x-6" />
                <div className="text-4xl mb-3">{dest.emoji}</div>
                <div className="font-bold text-white text-lg">{dest.city}</div>
                <div className="text-xs text-white/60 mt-1">{dest.tagline}</div>
                <div className="mt-3 text-xs font-semibold text-white/80 flex items-center gap-1">
                  {dest.code} <span className="opacity-50">→</span> Select
                </div>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ─────────────────────────────────────────────────────────── */}
      <section className="py-20 px-4">
        <div className="max-w-5xl mx-auto">
          <div className="text-center mb-14">
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-3">
              Why <span className="gradient-text">AeroLink?</span>
            </h2>
            <p className="text-gray-400">More than a booking engine — an intelligent travel partner</p>
          </div>
          <div className="grid md:grid-cols-3 gap-8">
            {FEATURES.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="glass rounded-2xl p-6 border border-white/8 card-hover text-center">
                <div className="w-12 h-12 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center mx-auto mb-4">
                  <Icon className="w-5 h-5 text-indigo-400" />
                </div>
                <h3 className="font-bold text-white mb-2">{title}</h3>
                <p className="text-sm text-gray-400 leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Stats ────────────────────────────────────────────────────────────── */}
      <section className="py-16 px-4 bg-navy border-t border-white/5">
        <div className="max-w-4xl mx-auto grid grid-cols-3 gap-8 text-center">
          {[
            { value: '500+', label: 'Airlines covered' },
            { value: '1M+',  label: 'Routes available' },
            { value: '24/7', label: 'AI assistant' },
          ].map(({ value, label }) => (
            <div key={label}>
              <div className="text-3xl md:text-4xl font-black gradient-text-gold">{value}</div>
              <div className="text-sm text-gray-400 mt-1">{label}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
