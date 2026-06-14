'use client';
import { useState, useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { User, Mail, Plane, CheckCircle, Loader2, Luggage, MapPin } from 'lucide-react';
import { bookFlight, getSeatMap, selectSeat, addBaggage } from '@/lib/api';
import SeatMap from '@/components/SeatMap';

const STEPS = ['Passenger', 'Seat', 'Baggage', 'Confirm'];

const BAGGAGE_OPTIONS = [
  { key: 'CHECKED_23KG', label: '23 kg Checked Bag',    fee: 35, icon: '🧳' },
  { key: 'CHECKED_32KG', label: '32 kg Checked Bag',    fee: 60, icon: '🧳' },
  { key: 'EXTRA_CARRYON', label: 'Extra Carry-On',       fee: 25, icon: '👜' },
  { key: 'SPORTS_EQUIPMENT', label: 'Sports Equipment',  fee: 70, icon: '🎿' },
];

function Step({ n, label, active, done }: { n: number; label: string; active: boolean; done: boolean }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-all ${
        done   ? 'bg-emerald-500 border-emerald-500 text-white' :
        active ? 'bg-indigo-600 border-indigo-500 text-white glow-sm' :
                 'bg-surface-2 border-white/10 text-gray-500'
      }`}>
        {done ? <CheckCircle className="w-4 h-4" /> : n}
      </div>
      <span className={`text-xs font-medium ${active ? 'text-indigo-300' : done ? 'text-emerald-300' : 'text-gray-500'}`}>{label}</span>
    </div>
  );
}

function BookingContent() {
  const sp = useSearchParams();
  const router = useRouter();

  const offerId      = sp.get('offer_id') ?? '';
  const origin       = sp.get('origin') ?? '';
  const destination  = sp.get('destination') ?? '';
  const departDate   = sp.get('departure_date') ?? '';
  const priceTotal   = sp.get('price_total') ?? '0';
  const currency     = sp.get('currency') ?? 'USD';
  const airline      = sp.get('airline') ?? '';
  const flightNum    = sp.get('flight_number') ?? '';
  const departTime   = sp.get('depart_time') ?? '';
  const arriveTime   = sp.get('arrive_time') ?? '';
  const cabin        = sp.get('cabin') ?? 'ECONOMY';

  const [step, setStep]     = useState(0);
  const [name, setName]     = useState('');
  const [email, setEmail]   = useState('');
  const [busy, setBusy]     = useState(false);
  const [error, setError]   = useState('');

  // Seat
  const [seatFees, setSeatFees]     = useState<Record<string,number>>({});
  const [seatType, setSeatType]     = useState('');
  const [seatNum,  setSeatNum]      = useState('');
  const [seatFee,  setSeatFee]      = useState(0);

  // Baggage
  const [bags, setBags] = useState<Array<{ key: string; qty: number }>>([]);

  // Result
  const [bookingId, setBookingId] = useState('');
  const [pnr, setPnr]             = useState('');

  useEffect(() => {
    getSeatMap(offerId).then(d => setSeatFees(d.seat_types)).catch(() => {});
  }, [offerId]);

  const totalExtras = seatFee + bags.reduce((s, b) => {
    const opt = BAGGAGE_OPTIONS.find(o => o.key === b.key);
    return s + (opt?.fee ?? 0) * b.qty;
  }, 0);
  const grandTotal = parseFloat(priceTotal) + totalExtras;

  function toggleBag(key: string) {
    setBags(prev => prev.find(b => b.key === key)
      ? prev.filter(b => b.key !== key)
      : [...prev, { key, qty: 1 }]
    );
  }

  async function confirmBooking() {
    setBusy(true); setError('');
    try {
      // 1. Book the flight
      const booked = await bookFlight({ offer_id: offerId, passenger_name: name, passenger_email: email, origin, destination, departure_date: departDate, price_total: priceTotal, currency });
      const bid = booked.booking_id;
      setBookingId(bid); setPnr(booked.pnr);

      // 2. Assign seat (if chosen)
      if (seatType) await selectSeat(bid, seatType, seatNum);

      // 3. Add baggage items
      for (const b of bags) await addBaggage(bid, b.key, b.qty);

      // Save to localStorage for My Trips
      const stored = JSON.parse(localStorage.getItem('aerolink_bookings') ?? '[]');
      stored.unshift({ booking_id: bid, pnr: booked.pnr, origin, destination, departure_date: departDate, airline, flight_number: flightNum });
      localStorage.setItem('aerolink_bookings', JSON.stringify(stored.slice(0, 20)));

      setStep(4);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Booking failed');
    } finally {
      setBusy(false);
    }
  }

  // ── Success screen ─────────────────────────────────────────────────────────
  if (step === 4) {
    return (
      <div className="min-h-screen pt-24 pb-12 px-4 flex items-center justify-center">
        <div className="max-w-md w-full text-center animate-slide-up">
          <div className="w-20 h-20 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="w-10 h-10 text-emerald-400" />
          </div>
          <h1 className="text-3xl font-black text-white mb-2">Booking Confirmed!</h1>
          <p className="text-gray-400 mb-8">You're all set. Here are your details.</p>
          <div className="glass rounded-2xl p-6 border border-white/10 text-left space-y-4 mb-6">
            <Row label="Booking ID" value={bookingId} mono />
            <Row label="PNR" value={pnr} mono />
            <Row label="Flight" value={`${flightNum} · ${airline}`} />
            <Row label="Route" value={`${origin} → ${destination}`} />
            <Row label="Date" value={departDate} />
            <Row label="Passenger" value={name} />
            <Row label="Total Paid" value={`$${grandTotal.toFixed(2)} ${currency}`} highlight />
          </div>
          <button onClick={() => router.push('/manage')}
            className="btn-primary w-full text-white font-bold py-3.5 rounded-xl">
            <span className="relative z-10">View My Trips</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen pt-24 pb-12 px-4">
      <div className="max-w-2xl mx-auto">
        {/* Flight summary strip */}
        <div className="glass rounded-2xl p-4 border border-white/8 mb-8 flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-600 to-purple-700 flex items-center justify-center font-black text-white text-sm">
              {airline.slice(0,2)}
            </div>
            <div>
              <div className="font-bold text-white text-sm">{flightNum}</div>
              <div className="text-xs text-gray-400">{cabin.replace('_',' ')}</div>
            </div>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="font-bold text-white">{departTime}</span>
            <span className="text-gray-500">{origin}</span>
            <Plane className="w-3.5 h-3.5 text-indigo-400" />
            <span className="text-gray-500">{destination}</span>
            <span className="font-bold text-white">{arriveTime}</span>
          </div>
          <div className="ml-auto">
            <div className="text-xl font-black gradient-text-gold">${parseFloat(priceTotal).toFixed(2)}</div>
            <div className="text-xs text-gray-500 text-right">base fare</div>
          </div>
        </div>

        {/* Step indicator */}
        <div className="flex items-center mb-10">
          {STEPS.map((s, i) => (
            <div key={s} className="flex items-center flex-1 last:flex-none">
              <Step n={i+1} label={s} active={step === i} done={step > i} />
              {i < STEPS.length - 1 && (
                <div className={`step-line mx-2 ${step > i ? 'active' : ''}`} />
              )}
            </div>
          ))}
        </div>

        {error && (
          <div className="mb-6 glass rounded-xl p-4 border border-red-500/30 text-red-300 text-sm">{error}</div>
        )}

        {/* ── Step 0: Passenger ─────────────────────────────────────────────── */}
        {step === 0 && (
          <div className="glass rounded-2xl p-6 border border-white/8 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
              <User className="w-5 h-5 text-indigo-400" /> Passenger Details
            </h2>
            <div className="space-y-4">
              <div>
                <label className="text-xs text-gray-400 mb-1.5 block">Full Name</label>
                <input value={name} onChange={e => setName(e.target.value)}
                  placeholder="John Doe"
                  className="input-dark w-full rounded-xl px-4 py-3" />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1.5 flex items-center gap-1"><Mail className="w-3 h-3" /> Email</label>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                  placeholder="john@example.com"
                  className="input-dark w-full rounded-xl px-4 py-3" />
              </div>
            </div>
            <button disabled={!name || !email}
              onClick={() => setStep(1)}
              className="btn-primary w-full mt-6 text-white font-bold py-3.5 rounded-xl disabled:opacity-40 disabled:cursor-not-allowed">
              <span className="relative z-10">Continue to Seat Selection</span>
            </button>
          </div>
        )}

        {/* ── Step 1: Seat map ──────────────────────────────────────────────── */}
        {step === 1 && (
          <div className="glass rounded-2xl p-6 border border-white/8 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
              <MapPin className="w-5 h-5 text-indigo-400" /> Choose Your Seat
            </h2>
            <SeatMap
              fees={seatFees}
              onSelect={(type, num, fee) => { setSeatType(type); setSeatNum(num); setSeatFee(fee); }}
            />
            <div className="flex gap-3 mt-6">
              <button onClick={() => { setSeatType(''); setSeatNum(''); setSeatFee(0); setStep(2); }}
                className="flex-1 glass border border-white/10 rounded-xl py-3 text-gray-300 hover:text-white text-sm font-medium transition-colors">
                Skip (No seat preference)
              </button>
              <button onClick={() => setStep(2)}
                className="flex-1 btn-primary text-white font-bold py-3 rounded-xl">
                <span className="relative z-10">{seatType ? `Confirm ${seatNum}` : 'Continue'}</span>
              </button>
            </div>
          </div>
        )}

        {/* ── Step 2: Baggage ───────────────────────────────────────────────── */}
        {step === 2 && (
          <div className="glass rounded-2xl p-6 border border-white/8 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
              <Luggage className="w-5 h-5 text-indigo-400" /> Add Baggage
            </h2>
            <div className="grid gap-3">
              {BAGGAGE_OPTIONS.map(opt => {
                const selected = !!bags.find(b => b.key === opt.key);
                return (
                  <button key={opt.key} onClick={() => toggleBag(opt.key)}
                    className={`flex items-center gap-4 p-4 rounded-xl border transition-all ${
                      selected
                        ? 'border-indigo-500/60 bg-indigo-600/15'
                        : 'border-white/8 hover:border-white/20 glass'
                    }`}>
                    <span className="text-2xl">{opt.icon}</span>
                    <div className="text-left flex-1">
                      <div className="font-medium text-white text-sm">{opt.label}</div>
                    </div>
                    <div className={`text-sm font-bold ${selected ? 'text-indigo-300' : 'text-gray-400'}`}>
                      +${opt.fee}
                    </div>
                    <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all ${
                      selected ? 'border-indigo-500 bg-indigo-600' : 'border-gray-600'
                    }`}>
                      {selected && <CheckCircle className="w-3 h-3 text-white" />}
                    </div>
                  </button>
                );
              })}
            </div>
            <div className="flex gap-3 mt-6">
              <button onClick={() => { setBags([]); setStep(3); }}
                className="flex-1 glass border border-white/10 rounded-xl py-3 text-gray-300 hover:text-white text-sm font-medium transition-colors">
                No Baggage
              </button>
              <button onClick={() => setStep(3)}
                className="flex-1 btn-primary text-white font-bold py-3 rounded-xl">
                <span className="relative z-10">Continue</span>
              </button>
            </div>
          </div>
        )}

        {/* ── Step 3: Review & confirm ──────────────────────────────────────── */}
        {step === 3 && (
          <div className="glass rounded-2xl p-6 border border-white/8 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-6">Review & Confirm</h2>
            <div className="space-y-3 mb-6">
              <Row label="Passenger" value={name} />
              <Row label="Email"     value={email} />
              <Row label="Route"     value={`${origin} → ${destination}`} />
              <Row label="Date"      value={departDate} />
              <Row label="Flight"    value={`${flightNum} (${cabin.replace('_',' ')})`} />
              {seatType && <Row label="Seat"    value={`${seatNum} (${seatType.replace('_',' ')}) +$${seatFee}`} />}
              {bags.map(b => {
                const opt = BAGGAGE_OPTIONS.find(o => o.key === b.key)!;
                return <Row key={b.key} label="Baggage" value={`${opt.label} +$${opt.fee}`} />;
              })}
              <div className="border-t border-white/10 pt-3 mt-3">
                <Row label="Total" value={`$${grandTotal.toFixed(2)} ${currency}`} highlight />
              </div>
            </div>
            <button onClick={confirmBooking} disabled={busy}
              className="btn-primary w-full text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 disabled:opacity-60">
              <span className="relative z-10 flex items-center gap-2">
                {busy ? <><Loader2 className="w-4 h-4 animate-spin" /> Confirming…</> : '✓ Confirm Booking'}
              </span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function Row({ label, value, mono, highlight }: { label: string; value: string; mono?: boolean; highlight?: boolean }) {
  return (
    <div className="flex items-start justify-between gap-4 text-sm">
      <span className="text-gray-400 shrink-0">{label}</span>
      <span className={`text-right font-medium ${mono ? 'font-mono text-indigo-300' : highlight ? 'text-amber-400 font-bold' : 'text-white'}`}>{value}</span>
    </div>
  );
}

export default function BookingPage() {
  return (
    <Suspense fallback={<div className="min-h-screen pt-24 flex items-center justify-center"><Loader2 className="w-8 h-8 text-indigo-400 animate-spin" /></div>}>
      <BookingContent />
    </Suspense>
  );
}
