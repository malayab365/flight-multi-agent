'use client';
import { useRouter } from 'next/navigation';
import { Plane, Clock, Users, ChevronRight, Zap } from 'lucide-react';
import type { FlightOffer } from '@/lib/api';

interface Props {
  offer: FlightOffer;
  searchParams: {
    origin: string;
    destination: string;
    departure_date: string;
    adults: number;
    travel_class: string;
  };
}

const AIRLINE_COLORS: Record<string, string> = {
  AA: 'from-blue-600 to-blue-800',
  DL: 'from-red-600 to-red-800',
  UA: 'from-sky-600 to-sky-800',
  BA: 'from-indigo-600 to-blue-800',
  EK: 'from-red-700 to-rose-900',
  LH: 'from-yellow-600 to-amber-800',
};

const AIRLINE_NAMES: Record<string, string> = {
  AA: 'American Airlines', DL: 'Delta Air Lines', UA: 'United Airlines',
  BA: 'British Airways', EK: 'Emirates', LH: 'Lufthansa',
};

function formatTime(t: string) {
  if (!t) return '--:--';
  if (t.includes('T')) {
    const d = new Date(t);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  }
  return t;
}

function calcDuration(dep: string, arr: string) {
  try {
    const d1 = dep.includes('T') ? new Date(dep) : new Date(`2026-01-01T${dep}`);
    const d2 = arr.includes('T') ? new Date(arr) : new Date(`2026-01-01T${arr}`);
    const diff = Math.abs(d2.getTime() - d1.getTime());
    const h = Math.floor(diff / 3_600_000);
    const m = Math.floor((diff % 3_600_000) / 60_000);
    return `${h}h ${m}m`;
  } catch { return ''; }
}

export default function FlightCard({ offer, searchParams }: Props) {
  const router = useRouter();
  const color = AIRLINE_COLORS[offer.airline] || 'from-indigo-600 to-purple-800';
  const name  = AIRLINE_NAMES[offer.airline] || offer.airline;
  const dep   = formatTime(offer.depart_time);
  const arr   = formatTime(offer.arrive_time);
  const dur   = calcDuration(offer.depart_time, offer.arrive_time);

  function handleBook() {
    const p = new URLSearchParams({
      offer_id:      offer.offer_id,
      origin:        offer.origin,
      destination:   offer.destination,
      departure_date:offer.departure_date,
      price_total:   offer.price.total,
      currency:      offer.price.currency,
      airline:       name,
      flight_number: offer.flight_number,
      depart_time:   dep,
      arrive_time:   arr,
      cabin:         offer.cabin,
    });
    router.push(`/booking?${p.toString()}`);
  }

  return (
    <div className="glass rounded-2xl overflow-hidden card-hover border border-white/8 group">
      <div className="flex flex-col md:flex-row">
        {/* Airline strip */}
        <div className={`bg-gradient-to-br ${color} px-5 py-4 flex md:flex-col items-center justify-between md:justify-center gap-3 md:w-36 md:min-h-full`}>
          <div className="text-center">
            <div className="text-2xl font-black text-white">{offer.airline}</div>
            <div className="text-xs text-white/70 mt-0.5">{offer.flight_number}</div>
          </div>
          <div className="text-xs text-white/60 text-center hidden md:block">{name}</div>
        </div>

        {/* Flight info */}
        <div className="flex-1 p-5 flex flex-col md:flex-row items-start md:items-center gap-4">
          {/* Times */}
          <div className="flex items-center gap-4 flex-1">
            <div className="text-center">
              <div className="text-2xl font-bold text-white">{dep}</div>
              <div className="text-xs text-gray-400 mt-0.5">{offer.origin}</div>
            </div>

            <div className="flex-1 flex flex-col items-center gap-1 min-w-[80px]">
              {dur && <div className="text-xs text-gray-500">{dur}</div>}
              <div className="relative w-full flex items-center">
                <div className="h-px flex-1 bg-gradient-to-r from-transparent via-indigo-500 to-transparent" />
                <Plane className="w-3.5 h-3.5 text-indigo-400 absolute left-1/2 -translate-x-1/2 -translate-y-0.5" />
              </div>
              <div className="text-xs text-gray-500">
                {offer.stops === 0 ? (
                  <span className="text-emerald-400 font-medium">Nonstop</span>
                ) : (
                  <span className="text-amber-400">{offer.stops} stop{offer.stops > 1 ? 's' : ''}</span>
                )}
              </div>
            </div>

            <div className="text-center">
              <div className="text-2xl font-bold text-white">{arr}</div>
              <div className="text-xs text-gray-400 mt-0.5">{offer.destination}</div>
            </div>
          </div>

          {/* Meta */}
          <div className="flex md:flex-col items-center md:items-end gap-4 md:gap-2">
            <div className="flex items-center gap-1.5 text-xs text-gray-400">
              <Users className="w-3.5 h-3.5" />
              <span>{offer.cabin.replace('_', ' ')}</span>
            </div>
            {offer.seats_remaining !== null && offer.seats_remaining <= 5 && (
              <div className="flex items-center gap-1 text-xs text-amber-400">
                <Zap className="w-3 h-3" />
                {offer.seats_remaining} left
              </div>
            )}
          </div>

          {/* Price + CTA */}
          <div className="flex md:flex-col items-center md:items-end gap-3 md:gap-2 ml-auto">
            <div className="text-right">
              <div className="text-2xl font-black gradient-text-gold">
                {offer.price.currency === 'USD' ? '$' : offer.price.currency}
                {parseFloat(offer.price.total).toFixed(0)}
              </div>
              <div className="text-xs text-gray-500">per person</div>
            </div>
            <button
              onClick={handleBook}
              className="btn-primary relative text-white text-sm font-semibold px-5 py-2.5 rounded-xl flex items-center gap-1.5 whitespace-nowrap"
            >
              <span className="relative z-10">Select</span>
              <ChevronRight className="w-4 h-4 relative z-10 group-hover:translate-x-0.5 transition-transform" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
