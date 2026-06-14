'use client';
import { useState } from 'react';
import { TrendingDown, Bell, BarChart3, CheckCircle, AlertCircle, Loader2, RefreshCw } from 'lucide-react';
import { trackPrice, checkPriceWatch, getPriceHistory, type PriceHistory, type PriceWatchCheck } from '@/lib/api';

interface Watch { watch_id: string; route: string; target: number; email: string; }

function PriceBar({ label, value, min, max, color }: { label: string; value: number; min: number; max: number; color: string }) {
  const pct = Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100));
  return (
    <div>
      <div className="flex justify-between text-xs text-gray-400 mb-1">
        <span>{label}</span><span className="font-mono font-semibold text-white">${value.toFixed(0)}</span>
      </div>
      <div className="h-2 bg-white/5 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-700 ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export default function TrackerPage() {
  const [origin, setOrigin] = useState('');
  const [dest, setDest]     = useState('');
  const [date, setDate]     = useState('');
  const [target, setTarget] = useState('');
  const [email, setEmail]   = useState('');
  const [busy, setBusy]     = useState(false);
  const [watches, setWatches] = useState<Watch[]>([]);
  const [checks, setChecks]   = useState<Record<string, PriceWatchCheck>>({});
  const [history, setHistory] = useState<PriceHistory | null>(null);
  const [loadingHist, setLoadingHist] = useState(false);
  const [error, setError]   = useState('');
  const [success, setSuccess] = useState('');

  async function handleTrack(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true); setError(''); setSuccess('');
    try {
      const res = await trackPrice({ origin: origin.toUpperCase(), destination: dest.toUpperCase(), departure_date: date, target_price: parseFloat(target), email });
      setWatches(prev => [{ watch_id: res.watch_id, route: `${origin.toUpperCase()}-${dest.toUpperCase()}`, target: parseFloat(target), email }, ...prev]);
      setSuccess(`Watch created! ID: ${res.watch_id}`);
      await loadHistory();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create watch');
    } finally { setBusy(false); }
  }

  async function loadHistory() {
    if (!origin || !dest || !date) return;
    setLoadingHist(true);
    try {
      const h = await getPriceHistory(origin.toUpperCase(), dest.toUpperCase(), date);
      setHistory(h);
    } catch {} finally { setLoadingHist(false); }
  }

  async function checkWatch(id: string) {
    try {
      const res = await checkPriceWatch(id);
      setChecks(prev => ({ ...prev, [id]: res }));
    } catch {}
  }

  return (
    <div className="min-h-screen pt-24 pb-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-1">Price Tracker</h1>
          <p className="text-gray-400 text-sm">Set fare alerts and track price history</p>
        </div>

        {/* Create watch form */}
        <div className="glass rounded-2xl p-6 border border-white/8 mb-6">
          <h2 className="text-lg font-bold text-white mb-5 flex items-center gap-2">
            <Bell className="w-5 h-5 text-indigo-400" /> New Price Alert
          </h2>
          <form onSubmit={handleTrack} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">From (IATA)</label>
                <input value={origin} onChange={e => setOrigin(e.target.value.toUpperCase())}
                  placeholder="JFK" className="input-dark w-full rounded-xl px-3 py-2.5 text-sm uppercase" required />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">To (IATA)</label>
                <input value={dest} onChange={e => setDest(e.target.value.toUpperCase())}
                  placeholder="LHR" className="input-dark w-full rounded-xl px-3 py-2.5 text-sm uppercase" required />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Departure Date</label>
                <input type="date" value={date} onChange={e => setDate(e.target.value)}
                  className="input-dark w-full rounded-xl px-3 py-2.5 text-sm" required />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Alert me below ($)</label>
                <input type="number" value={target} onChange={e => setTarget(e.target.value)}
                  placeholder="200" className="input-dark w-full rounded-xl px-3 py-2.5 text-sm" required />
              </div>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Your email</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                placeholder="you@example.com" className="input-dark w-full rounded-xl px-3 py-2.5 text-sm" required />
            </div>

            {error   && <p className="text-red-400 text-sm flex items-center gap-1"><AlertCircle className="w-4 h-4" />{error}</p>}
            {success && <p className="text-emerald-400 text-sm flex items-center gap-1"><CheckCircle className="w-4 h-4" />{success}</p>}

            <div className="flex gap-3">
              <button type="button" onClick={loadHistory} disabled={!origin || !dest || !date || loadingHist}
                className="glass border border-white/10 rounded-xl px-4 py-2.5 text-sm text-gray-300 hover:text-white transition-colors flex items-center gap-1.5 disabled:opacity-40">
                <BarChart3 className="w-4 h-4" /> {loadingHist ? 'Loading…' : 'View History'}
              </button>
              <button type="submit" disabled={busy}
                className="btn-primary flex-1 text-white font-bold py-2.5 rounded-xl text-sm flex items-center justify-center gap-2">
                <span className="relative z-10 flex items-center gap-2">
                  {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <Bell className="w-4 h-4" />}
                  {busy ? 'Creating…' : 'Create Alert'}
                </span>
              </button>
            </div>
          </form>
        </div>

        {/* Price history */}
        {history && (
          <div className="glass rounded-2xl p-6 border border-white/8 mb-6 animate-fade-in">
            <h2 className="text-base font-bold text-white mb-4 flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-indigo-400" />
              Historical Fares — {history.route}
            </h2>
            <div className="space-y-3">
              <PriceBar label="Cheapest seen" value={history.quartiles.minimum} min={history.quartiles.minimum} max={history.quartiles.maximum} color="bg-emerald-500" />
              <PriceBar label="Lower quarter" value={history.quartiles.first}   min={history.quartiles.minimum} max={history.quartiles.maximum} color="bg-teal-500" />
              <PriceBar label="Median"        value={history.quartiles.median}  min={history.quartiles.minimum} max={history.quartiles.maximum} color="bg-indigo-500" />
              <PriceBar label="Upper quarter" value={history.quartiles.third}   min={history.quartiles.minimum} max={history.quartiles.maximum} color="bg-amber-500" />
              <PriceBar label="Most expensive" value={history.quartiles.maximum} min={history.quartiles.minimum} max={history.quartiles.maximum} color="bg-red-500" />
            </div>
            {target && (
              <div className="mt-4 text-xs text-gray-400 border-t border-white/8 pt-3">
                Your target ${parseFloat(target).toFixed(0)} is{' '}
                {parseFloat(target) <= history.quartiles.first
                  ? <span className="text-emerald-400 font-semibold">an excellent deal — below the lower quarter</span>
                  : parseFloat(target) <= history.quartiles.median
                  ? <span className="text-teal-400 font-semibold">a good deal — below the median</span>
                  : <span className="text-amber-400 font-semibold">above the median — try going lower</span>
                }
              </div>
            )}
          </div>
        )}

        {/* Active watches */}
        {watches.length > 0 && (
          <div className="glass rounded-2xl p-6 border border-white/8">
            <h2 className="text-base font-bold text-white mb-4 flex items-center gap-2">
              <TrendingDown className="w-4 h-4 text-indigo-400" /> Active Alerts
            </h2>
            <div className="space-y-3">
              {watches.map(w => {
                const c = checks[w.watch_id];
                return (
                  <div key={w.watch_id} className="rounded-xl border border-white/8 p-4 bg-white/2">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="font-semibold text-white text-sm">{w.route}</div>
                        <div className="text-xs text-gray-500 font-mono mt-0.5">{w.watch_id}</div>
                        <div className="text-xs text-gray-400 mt-1">Alert at ≤ <span className="text-amber-400 font-semibold">${w.target}</span> → {w.email}</div>
                      </div>
                      <button onClick={() => checkWatch(w.watch_id)}
                        className="glass border border-white/10 rounded-lg p-1.5 text-gray-400 hover:text-white transition-colors">
                        <RefreshCw className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    {c && (
                      <div className={`mt-3 rounded-lg px-3 py-2 text-xs flex items-center gap-2 ${
                        c.alert_triggered
                          ? 'bg-emerald-500/20 border border-emerald-500/30 text-emerald-300'
                          : 'bg-white/4 border border-white/8 text-gray-400'
                      }`}>
                        {c.alert_triggered ? <Bell className="w-3.5 h-3.5" /> : <TrendingDown className="w-3.5 h-3.5" />}
                        Current price: <strong className="text-white">${c.current_price.toFixed(2)}</strong>
                        {c.alert_triggered ? ' — Alert triggered! 🎉' : ' — Still watching'}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
