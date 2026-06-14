const BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || `Request failed: ${res.status}`);
  }
  return res.json();
}

// ── Types ─────────────────────────────────────────────────────────────────────

export interface FlightOffer {
  offer_id: string;
  airline: string;
  flight_number: string;
  origin: string;
  destination: string;
  departure_date: string;
  depart_time: string;
  arrive_time: string;
  stops: number;
  cabin: string;
  price: { total: string; currency: string };
  seats_remaining: number | null;
}

export interface SearchResult {
  source: string;
  offers: FlightOffer[];
}

export interface Booking {
  booking_id: string;
  offer_id: string;
  pnr: string;
  status: string;
  passenger: { name: string; email: string };
  itinerary: { origin: string; destination: string; departure_date: string };
  price: { total: string; currency: string };
  seat: { type: string; number: string | null; fee: number } | null;
  baggage: Array<{ type: string; quantity: number; fee: number }>;
  created_at: string;
}

export interface SeatMap {
  offer_id: string;
  currency: string;
  seat_types: Record<string, number>;
}

export interface PriceHistory {
  route: string;
  departure_date: string;
  currency: string;
  quartiles: {
    minimum: number; first: number; median: number; third: number; maximum: number;
  };
}

export interface PriceWatch {
  watch_id: string;
  status: string;
  message: string;
}

export interface PriceWatchCheck {
  watch_id: string;
  route: string;
  current_price: number;
  target_price: number;
  alert_triggered: boolean;
  action: string;
}

export interface ChatResponse {
  response: string;
  agent: string;
}

// ── Chat ─────────────────────────────────────────────────────────────────────

export function sendChat(message: string, history: Array<{ role: string; content: string }>) {
  return request<ChatResponse>('/chat', {
    method: 'POST',
    body: JSON.stringify({ message, history }),
  });
}

// ── Search ────────────────────────────────────────────────────────────────────

export interface SearchParams {
  origin: string;
  destination: string;
  departure_date: string;
  return_date?: string;
  adults?: number;
  travel_class?: string;
  max_results?: number;
}

export function searchFlights(params: SearchParams) {
  return request<SearchResult>('/search', { method: 'POST', body: JSON.stringify(params) });
}

export function getAirportCode(city: string) {
  return request<{ source: string; matches: Array<{ name: string; iata: string; country?: string }> }>(
    `/airport-code?city=${encodeURIComponent(city)}`
  );
}

// ── Booking ───────────────────────────────────────────────────────────────────

export interface BookParams {
  offer_id: string;
  passenger_name: string;
  passenger_email: string;
  origin: string;
  destination: string;
  departure_date: string;
  price_total: string;
  currency?: string;
}

export function bookFlight(params: BookParams) {
  return request<{ booking_id: string; pnr: string; status: string; summary: string }>(
    '/book', { method: 'POST', body: JSON.stringify(params) }
  );
}

export function getBooking(id: string) {
  return request<Booking>(`/booking/${id}`);
}

export function cancelBooking(id: string, reason?: string) {
  return request<{ booking_id: string; status: string; refund_estimate?: { amount: number; currency: string } }>(
    `/booking/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }
  );
}

export function modifyBooking(id: string, params: { new_departure_date?: string; new_destination?: string }) {
  return request<{ booking_id: string; status: string; changes: Record<string, string>; change_fee: { amount: number; currency: string } }>(
    `/booking/${id}/modify`, { method: 'POST', body: JSON.stringify(params) }
  );
}

// ── Ancillary ─────────────────────────────────────────────────────────────────

export function getSeatMap(offerId: string) {
  return request<SeatMap>(`/seat-map/${offerId}`);
}

export function selectSeat(bookingId: string, seat_type: string, seat_number?: string) {
  return request<{ booking_id: string; seat: { type: string; number: string; fee: number }; status: string }>(
    `/booking/${bookingId}/seat`, { method: 'POST', body: JSON.stringify({ seat_type, seat_number }) }
  );
}

export function addBaggage(bookingId: string, baggage_type: string, quantity: number) {
  return request<{ booking_id: string; added: { type: string; quantity: number; fee: number }; all_baggage: unknown[]; status: string }>(
    `/booking/${bookingId}/baggage`, { method: 'POST', body: JSON.stringify({ baggage_type, quantity }) }
  );
}

// ── Price ─────────────────────────────────────────────────────────────────────

export function trackPrice(params: { origin: string; destination: string; departure_date: string; target_price: number; email: string }) {
  return request<PriceWatch>('/price/track', { method: 'POST', body: JSON.stringify(params) });
}

export function checkPriceWatch(watchId: string) {
  return request<PriceWatchCheck>(`/price/watch/${watchId}`);
}

export function getPriceHistory(origin: string, destination: string, departure_date: string) {
  return request<PriceHistory>(`/price/history?origin=${origin}&destination=${destination}&departure_date=${departure_date}`);
}

// ── Info ──────────────────────────────────────────────────────────────────────

export function getWeather(city: string) {
  return request<{ source: string; city: string; temp_c: number; conditions: string; humidity: number }>(
    `/weather/${encodeURIComponent(city)}`
  );
}

export function getDestinationInfo(city: string) {
  return request<{ city: string; info: Record<string, string> }>(
    `/destination/${encodeURIComponent(city)}`
  );
}
