'use client';
import { useState, useEffect } from 'react';
import { Plane, X, Edit3, ChevronDown, ChevronUp, Loader2, AlertCircle, CheckCircle, Luggage } from 'lucide-react';
import { getBooking, cancelBooking, modifyBooking, type Booking } from '@/lib/api';

interface StoredBooking {
  booking_id: string; pnr: string; origin: string; destination: string;
  departure_date: string; airline: string; flight_number: string;
}

const STATUS_STYLES: Record<string, string> = {
  CONFIRMED: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
  MODIFIED:  'bg-blue-500/20 text-blue-300 border-blue-500/30',
  CANCELLED: 'bg-red-500/20 text-red-300 border-red-500/30',
};

export default function ManagePage() {
  const [stored, setStored] = useState<StoredBooking[]>([]);
  const [bookings, setBookings] = useState<Record<string, Booking>>({});
  const [expanded, setExpanded] = useState<string | null>(null);
  const [loading, setLoading] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<Record<string, string>>({});
  const [lookupId, setLookupId] = useState('');
  const [modModal, setModModal] = useState<string | null>(null);
  const [newDate, setNewDate] = useState('');
  const [newDest, setNewDest] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const s = JSON.parse(localStorage.getItem('aerolink_bookings') ?? '[]');
    setStored(s);
  }, []);

  async function loadBooking(id: string) {
    if (bookings[id]) { setExpanded(id === expanded ? null : id); return; }
    setLoading(l => ({ ...l, [id]: true })); setError(e => ({ ...e, [id]: '' }));
    try {
      const b = await getBooking(id);
      setBookings(prev => ({ ...prev, [id]: b }));
      setExpanded(id);
    } catch (e: unknown) {
      setError(prev => ({ ...prev, [id]: e instanceof Error ? e.message : 'Not found' }));
    } finally {
      setLoading(l => ({ ...l, [id]: false }));
    }
  }

  async function handleCancel(id: string) {
    if (!confirm('Cancel this booking? An 80% refund will be issued.')) return;
    setBusy(true);
    try {
      const res = await cancelBooking(id, 'User requested cancellation');
      setBookings(prev => ({ ...prev, [id]: { ...prev[id], status: res.status } }));
    } finally { setBusy(false); }
  }

  async function handleModify(id: string) {
    setBusy(true);
    try {
      const res = await modifyBooking(id, {
        new_departure_date: newDate || undefined,
        new_destination: newDest.toUpperCase() || undefined,
      });
      const updated = { ...bookings[id], status: res.status };
      if (res.changes.destination) updated.itinerary = { ...updated.itinerary, destination: res.changes.destination };
      if (res.changes.departure_date) updated.itinerary = { ...updated.itinerary, departure_date: res.changes.departure_date };
      setBookings(prev => ({ ...prev, [id]: updated }));
      setModModal(null); setNewDate(''); setNewDest('');
    } finally { setBusy(false); }
  }

  async function handleLookup(e: React.FormEvent) {
    e.preventDefault();
    if (!lookupId.trim()) return;
    const id = lookupId.trim().toUpperCase();
    if (!stored.find(s => s.booking_id === id)) {
      setStored(prev => [...prev, { booking_id: id, pnr: '', origin: '', destination: '', departure_date: '', airline: '', flight_number: '' }]);
    }
    await loadBooking(id);
    setLookupId('');
  }

  return (
    <div className="min-h-screen pt-24 pb-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-1">My Trips</h1>
          <p className="text-gray-400 text-sm">View and manage your bookings</p>
        </div>

        {/* Lookup form */}
        <form onSubmit={handleLookup} className="glass rounded-2xl p-4 border border-white/8 mb-6 flex gap-3">
          <input
            value={lookupId} onChange={e => setLookupId(e.target.value)}
            placeholder="Enter Booking ID (e.g. BK-A3F29C1B)"
            className="input-dark flex-1 rounded-xl px-4 py-2.5 text-sm font-mono"
          />
          <button type="submit" className="btn-primary text-white font-semibold px-5 py-2.5 rounded-xl text-sm">
            <span className="relative z-10">Look up</span>
          </button>
        </form>

        {stored.length === 0 && (
          <div className="text-center py-20">
            <Plane className="w-12 h-12 mx-auto mb-4 text-gray-700" />
            <p className="text-gray-400">No bookings yet. Search for a flight to get started.</p>
          </div>
        )}

        <div className="space-y-4">
          {stored.map(s => {
            const b = bookings[s.booking_id];
            const isExpanded = expanded === s.booking_id;
            const statusStyle = STATUS_STYLES[b?.status ?? 'CONFIRMED'] ?? STATUS_STYLES.CONFIRMED;

            return (
              <div key={s.booking_id} className="glass rounded-2xl border border-white/8 overflow-hidden card-hover">
                {/* Card header */}
                <button onClick={() => loadBooking(s.booking_id)}
                  className="w-full flex items-center gap-4 p-5 text-left">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-600 to-purple-700 flex items-center justify-center font-black text-white text-sm shrink-0">
                    {(b?.itinerary.origin ?? s.origin)?.slice(0,2) || '✈'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold text-white truncate">
                      {b?.itinerary.origin ?? s.origin} → {b?.itinerary.destination ?? s.destination}
                    </div>
                    <div className="text-xs text-gray-400 mt-0.5 font-mono">{s.booking_id}</div>
                  </div>
                  {b?.status && (
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${statusStyle} shrink-0`}>
                      {b.status}
                    </span>
                  )}
                  <div className="text-gray-500">
                    {loading[s.booking_id]
                      ? <Loader2 className="w-4 h-4 animate-spin" />
                      : isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </div>
                </button>

                {error[s.booking_id] && (
                  <div className="px-5 pb-4 text-sm text-red-400 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4" /> {error[s.booking_id]}
                  </div>
                )}

                {/* Expanded detail */}
                {isExpanded && b && (
                  <div className="border-t border-white/8 px-5 py-5 space-y-4 animate-fade-in">
                    <div className="grid grid-cols-2 gap-3 text-sm">
                      <Info label="PNR" value={b.pnr} mono />
                      <Info label="Date" value={b.itinerary.departure_date} />
                      <Info label="Passenger" value={b.passenger.name} />
                      <Info label="Email" value={b.passenger.email} />
                      <Info label="Total Price" value={`$${b.price.total} ${b.price.currency}`} highlight />
                      <Info label="Created" value={new Date(b.created_at).toLocaleDateString()} />
                    </div>

                    {b.seat && (
                      <div className="glass rounded-xl px-4 py-3 border border-white/8 flex items-center gap-3 text-sm">
                        <CheckCircle className="w-4 h-4 text-emerald-400" />
                        <span className="text-gray-300">Seat <strong className="text-white">{b.seat.number ?? b.seat.type}</strong> ({b.seat.type.replace('_',' ')}) — ${b.seat.fee}</span>
                      </div>
                    )}

                    {b.baggage.length > 0 && (
                      <div className="glass rounded-xl px-4 py-3 border border-white/8 text-sm">
                        <div className="flex items-center gap-2 mb-2 text-gray-400">
                          <Luggage className="w-4 h-4" /> Baggage
                        </div>
                        {b.baggage.map((bag, i) => (
                          <div key={i} className="text-white text-xs">{bag.type.replace(/_/g,' ')} ×{bag.quantity} — ${bag.fee}</div>
                        ))}
                      </div>
                    )}

                    {b.status !== 'CANCELLED' && (
                      <div className="flex gap-2 pt-2">
                        <button onClick={() => { setModModal(s.booking_id); setNewDate(b.itinerary.departure_date); setNewDest(b.itinerary.destination); }}
                          className="flex-1 flex items-center justify-center gap-1.5 glass border border-white/10 rounded-xl py-2.5 text-sm text-gray-300 hover:text-white hover:border-indigo-500/40 transition-all">
                          <Edit3 className="w-3.5 h-3.5" /> Modify
                        </button>
                        <button onClick={() => handleCancel(s.booking_id)} disabled={busy}
                          className="flex-1 flex items-center justify-center gap-1.5 glass border border-red-500/20 rounded-xl py-2.5 text-sm text-red-400 hover:text-red-300 hover:border-red-500/50 transition-all disabled:opacity-40">
                          <X className="w-3.5 h-3.5" /> Cancel
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Modify modal */}
      {modModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <div className="glass rounded-2xl p-6 border border-white/10 w-full max-w-sm animate-slide-up">
            <h3 className="text-lg font-bold text-white mb-4">Modify Booking</h3>
            <p className="text-xs text-amber-400 mb-4">⚠ A $75 change fee applies</p>
            <div className="space-y-3 mb-5">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">New departure date</label>
                <input type="date" value={newDate} onChange={e => setNewDate(e.target.value)}
                  className="input-dark w-full rounded-xl px-3 py-2.5 text-sm" />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">New destination (IATA)</label>
                <input value={newDest} onChange={e => setNewDest(e.target.value.toUpperCase())}
                  placeholder="e.g. CDG"
                  className="input-dark w-full rounded-xl px-3 py-2.5 text-sm uppercase" />
              </div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setModModal(null)}
                className="flex-1 glass border border-white/10 rounded-xl py-2.5 text-sm text-gray-300">Cancel</button>
              <button onClick={() => handleModify(modModal)} disabled={busy}
                className="flex-1 btn-primary text-white font-bold py-2.5 rounded-xl text-sm">
                <span className="relative z-10">{busy ? 'Saving…' : 'Confirm'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Info({ label, value, mono, highlight }: { label: string; value: string; mono?: boolean; highlight?: boolean }) {
  return (
    <div>
      <div className="text-xs text-gray-500 mb-0.5">{label}</div>
      <div className={`font-medium ${mono ? 'font-mono text-indigo-300 text-xs' : highlight ? 'text-amber-400' : 'text-white'}`}>{value}</div>
    </div>
  );
}
