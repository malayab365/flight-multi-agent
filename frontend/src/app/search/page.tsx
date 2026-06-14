'use client';
import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'next/navigation';
import { Plane, SlidersHorizontal, ArrowUpDown, AlertCircle, Loader2 } from 'lucide-react';
import { searchFlights, type FlightOffer, type SearchResult } from '@/lib/api';
import FlightCard from '@/components/FlightCard';
import { Suspense } from 'react';

const SORT_OPTIONS = [
  { value: 'price-asc',  label: 'Price (low → high)' },
  { value: 'price-desc', label: 'Price (high → low)' },
  { value: 'stops',      label: 'Fewest stops' },
];

function SkeletonCard() {
  return (
    <div className="glass rounded-2xl overflow-hidden border border-white/8 h-32">
      <div className="flex h-full">
        <div className="skeleton w-36 h-full" />
        <div className="flex-1 p-5 flex flex-col gap-3">
          <div className="skeleton h-4 w-1/2 rounded" />
          <div className="skeleton h-4 w-1/3 rounded" />
          <div className="skeleton h-4 w-1/4 rounded" />
        </div>
      </div>
    </div>
  );
}

function SearchContent() {
  const sp = useSearchParams();
  const origin        = sp.get('origin') ?? '';
  const destination   = sp.get('destination') ?? '';
  const departure_date= sp.get('departure_date') ?? '';
  const return_date   = sp.get('return_date') ?? undefined;
  const adults        = parseInt(sp.get('adults') ?? '1');
  const travel_class  = sp.get('travel_class') ?? 'ECONOMY';

  const [result,  setResult]  = useState<SearchResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [sort,    setSort]    = useState('price-asc');
  const [stopFilter, setStopFilter] = useState<number | null>(null);

  const doSearch = useCallback(async () => {
    if (!origin || !destination || !departure_date) return;
    setLoading(true); setError('');
    try {
      const data = await searchFlights({ origin, destination, departure_date, return_date, adults, travel_class, max_results: 8 });
      setResult(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }, [origin, destination, departure_date, return_date, adults, travel_class]);

  useEffect(() => { doSearch(); }, [doSearch]);

  const sorted = [...(result?.offers ?? [])].filter(o => stopFilter === null || o.stops === stopFilter).sort((a, b) => {
    if (sort === 'price-asc')  return parseFloat(a.price.total) - parseFloat(b.price.total);
    if (sort === 'price-desc') return parseFloat(b.price.total) - parseFloat(a.price.total);
    return a.stops - b.stops;
  });

  const searchContext = { origin, destination, departure_date, adults, travel_class };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8 animate-slide-up">
          <div className="flex items-center gap-3 text-sm text-gray-400 mb-2">
            <Plane className="w-4 h-4 text-indigo-400" />
            <span className="font-medium text-white">{origin}</span>
            <span>→</span>
            <span className="font-medium text-white">{destination}</span>
            <span className="text-gray-600">·</span>
            <span>{departure_date}</span>
            <span className="text-gray-600">·</span>
            <span>{adults} adult{adults > 1 ? 's' : ''}</span>
          </div>
          <h1 className="text-3xl font-bold text-white">
            {loading ? 'Searching…' : `${sorted.length} flight${sorted.length !== 1 ? 's' : ''} found`}
          </h1>
        </div>

        {/* Controls */}
        <div className="flex flex-wrap items-center gap-3 mb-6">
          {/* Stop filter */}
          <div className="flex items-center gap-1 glass rounded-xl p-1 border border-white/8">
            <SlidersHorizontal className="w-4 h-4 text-gray-400 ml-2" />
            {[null, 0, 1].map(s => (
              <button key={String(s)}
                onClick={() => setStopFilter(s)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  stopFilter === s ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
                }`}>
                {s === null ? 'All' : s === 0 ? 'Nonstop' : '1 stop'}
              </button>
            ))}
          </div>

          {/* Sort */}
          <div className="flex items-center gap-1 glass rounded-xl p-1 border border-white/8 ml-auto">
            <ArrowUpDown className="w-4 h-4 text-gray-400 ml-2" />
            {SORT_OPTIONS.map(o => (
              <button key={o.value}
                onClick={() => setSort(o.value)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  sort === o.value ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
                }`}>
                {o.label}
              </button>
            ))}
          </div>
        </div>

        {/* Source badge */}
        {result && (
          <div className="mb-4 text-xs text-gray-500 flex items-center gap-1.5">
            <div className={`w-2 h-2 rounded-full ${result.source === 'amadeus' ? 'bg-emerald-400' : 'bg-amber-400'}`} />
            {result.source === 'amadeus' ? 'Live Amadeus data' : 'Sample data — add Amadeus keys for live results'}
          </div>
        )}

        {/* Results */}
        {loading && (
          <div className="space-y-4">
            {[1,2,3].map(i => <SkeletonCard key={i} />)}
          </div>
        )}

        {error && (
          <div className="glass rounded-2xl p-6 border border-red-500/30 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-semibold text-red-300">Search failed</div>
              <div className="text-sm text-gray-400 mt-1">{error}</div>
              <button onClick={doSearch} className="mt-3 text-sm text-indigo-400 hover:text-indigo-300 underline">Try again</button>
            </div>
          </div>
        )}

        {!loading && !error && sorted.length === 0 && (
          <div className="text-center py-20 text-gray-400">
            <Plane className="w-12 h-12 mx-auto mb-4 opacity-20" />
            <p className="text-lg font-medium">No flights found</p>
            <p className="text-sm mt-1">Try changing your filters or search again</p>
          </div>
        )}

        <div className="space-y-4">
          {sorted.map((offer, i) => (
            <div key={offer.offer_id} style={{ animationDelay: `${i * 80}ms` }} className="animate-slide-up">
              <FlightCard offer={offer} searchParams={searchContext} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen pt-24 flex items-center justify-center">
        <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
      </div>
    }>
      <SearchContent />
    </Suspense>
  );
}
