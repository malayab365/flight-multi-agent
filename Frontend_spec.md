# AeroLink — Frontend Specification

## Table of Contents

1. [What the Frontend Is (Plain English)](#1-what-the-frontend-is-plain-english)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [How to Run](#4-how-to-run)
5. [Pages — What Each Screen Does](#5-pages--what-each-screen-does)
   - 5.1 [Home `/`](#51-home-)
   - 5.2 [Search Results `/search`](#52-search-results-search)
   - 5.3 [Booking Flow `/booking`](#53-booking-flow-booking)
   - 5.4 [My Trips `/manage`](#54-my-trips-manage)
   - 5.5 [Price Tracker `/tracker`](#55-price-tracker-tracker)
   - 5.6 [AI Assistant `/chat`](#56-ai-assistant-chat)
6. [Components](#6-components)
   - 6.1 [Navbar](#61-navbar)
   - 6.2 [FlightCard](#62-flightcard)
   - 6.3 [SeatMap](#63-seatmap)
7. [Design System](#7-design-system)
   - 7.1 [Colour Palette](#71-colour-palette)
   - 7.2 [Typography](#72-typography)
   - 7.3 [CSS Utility Classes](#73-css-utility-classes)
   - 7.4 [Animations & Motion](#74-animations--motion)
8. [API Client (`src/lib/api.ts`)](#8-api-client-srclibapits)
   - 8.1 [TypeScript Types](#81-typescript-types)
   - 8.2 [Available Functions](#82-available-functions)
9. [Backend Connection](#9-backend-connection)
10. [State Management](#10-state-management)
11. [Local Persistence](#11-local-persistence)
12. [Configuration Files](#12-configuration-files)
13. [Build Output](#13-build-output)
14. [Extending the Frontend](#14-extending-the-frontend)

---

## 1. What the Frontend Is (Plain English)

AeroLink's frontend is a web application that lets users interact with the flight multi-agent AI system through a polished browser interface instead of a command line. You open it in any browser, and you get a full travel experience:

- **Search for flights** between two cities on any date
- **Book a flight** by filling in your name and email
- **Choose your exact seat** on an interactive cabin map
- **Add baggage** options before confirming your reservation
- **Manage your bookings** — view details, modify dates, or cancel
- **Track prices** and set email alerts for fare drops
- **Chat freely** with the AI assistant, which automatically routes your request to whichever specialist agent fits best

The frontend talks to the Python backend (FastAPI server) over a local HTTP connection. It does not contain any AI or flight logic itself — it is purely responsible for displaying information and capturing user input.

---

## 2. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Next.js** | 14.2.18 | React framework, App Router, file-based routing, API proxy rewrites |
| **React** | 18 | UI component model, hooks for state and effects |
| **TypeScript** | 5 | Static typing across all components and API calls |
| **Tailwind CSS** | 3.4 | Utility-first styling, responsive layout, custom colour tokens |
| **Lucide React** | 0.462 | Icon library (Plane, Search, Bell, MessageSquare, etc.) |
| **clsx** | 2.1 | Conditional class name merging |
| **PostCSS / Autoprefixer** | — | CSS build pipeline |

No external animation library, no state management library (Redux / Zustand), no form library — the project is intentionally lean. All interactivity is handled with React's built-in `useState` and `useEffect` hooks.

---

## 3. Project Structure

```
frontend/
├── next.config.js              # API proxy rewrite rule (→ localhost:8000)
├── tailwind.config.js          # Custom tokens: colours, keyframes, animations
├── postcss.config.js           # Tailwind + Autoprefixer pipeline
├── tsconfig.json               # TypeScript config; path alias @/* → ./src/*
├── package.json                # Dependencies and npm scripts
├── .gitignore                  # Excludes node_modules, .next, .env.local
│
└── src/
    ├── app/                    # Next.js App Router pages
    │   ├── layout.tsx          # Root layout — Navbar + <main> wrapper
    │   ├── globals.css         # Tailwind base + all custom CSS classes
    │   ├── page.tsx            # Home page (/)
    │   ├── search/
    │   │   └── page.tsx        # Search results (/search)
    │   ├── booking/
    │   │   └── page.tsx        # Booking flow (/booking)
    │   ├── manage/
    │   │   └── page.tsx        # My Trips (/manage)
    │   ├── tracker/
    │   │   └── page.tsx        # Price Tracker (/tracker)
    │   └── chat/
    │       └── page.tsx        # AI Assistant (/chat)
    │
    ├── components/             # Shared reusable components
    │   ├── Navbar.tsx          # Fixed top navigation bar
    │   ├── FlightCard.tsx      # Single flight offer card
    │   └── SeatMap.tsx         # Interactive 30×6 seat grid
    │
    └── lib/
        └── api.ts              # All backend API calls + TypeScript types
```

Every file under `src/app/` is a **client component** (`'use client'` directive at the top), because they all use React state, event handlers, or browser APIs such as `localStorage`.

---

## 4. How to Run

### Prerequisites
- Node.js 18+
- The Python FastAPI backend running on port 8000 (see `src/api.py`)

### Steps

```bash
# 1. Start the Python backend (from the project root)
pip install -r requirements.txt
uvicorn src.api:app --reload --port 8000

# 2. In a separate terminal, start the frontend
cd frontend
npm install        # only needed once
npm run dev        # starts Next.js dev server on http://localhost:3000
```

Open **http://localhost:3000** in any browser.

### Available npm scripts

| Script | What it does |
|---|---|
| `npm run dev` | Development server with hot reload on port 3000 |
| `npm run build` | Compile and optimise for production |
| `npm run start` | Serve the production build |
| `npm run lint` | Run ESLint across all source files |

---

## 5. Pages — What Each Screen Does

### 5.1 Home `/`

**File:** `src/app/page.tsx`

The landing page. It serves as both the marketing introduction and the primary search entry point.

#### Sections

**Hero section (full viewport)**
- An animated aurora gradient background (`aurora-bg` CSS class) that slowly cycles between deep navy, indigo, and violet — achieved with a CSS `background-position` keyframe animation on a 300%-wide gradient.
- Two radial glow blobs (`blur-3xl`) placed top-left and bottom-right create a depth effect.
- A floating plane icon (`animate-float`) drifts up and down in the corner using a `translateY` keyframe.
- A pill badge reading "Powered by multi-agent AI" in glassmorphism style.
- A large headline: "Discover Your **World**" — "World" rendered with `gradient-text` (indigo→violet→pink).
- The flight search widget, embedded in a glassmorphism card (`glass` class with `backdrop-filter: blur(20px)`).

**Search widget behaviour**

| Control | Behaviour |
|---|---|
| One Way / Round Trip toggle | Adds or removes the Return Date field |
| From / To inputs | Accept IATA codes; auto-uppercased on change |
| Swap button (⇄) | Swaps the From and To values |
| Depart / Return date | Native `<input type="date">` |
| Passengers counter | +/− buttons; clamped between 1 and 9 |
| Class selector | Custom dropdown (not native `<select>`); options: ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST |
| Search button | Validates that From, To, and Depart are non-empty; builds a URL query string and calls `router.push('/search?...')` |

**Popular Destinations section**
- Four destination cards: Tokyo (NRT), London (LHR), Dubai (DXB), Paris (CDG).
- Each card has a unique gradient background, an emoji, a city name, and a tagline.
- Clicking a card sets the "To" field to that airport code and smooth-scrolls back to the top of the page.

**Why AeroLink section**
- Three feature cards: AI-Powered Search, Secure Booking, Real-Time Prices.
- Each card has a bordered icon container, a title, and a short description.
- Cards lift 4px on hover (`card-hover` class).

**Stats strip**
- Three counters: 500+ Airlines, 1M+ Routes, 24/7 AI Assistant.
- Numbers rendered with `gradient-text-gold` (amber gradient).

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `tripType` | `'one-way' \| 'round-trip'` | Controls whether the return date field is shown |
| `from`, `to` | `string` | Origin and destination IATA codes |
| `date`, `returnDate` | `string` | Date strings in YYYY-MM-DD format |
| `adults` | `number` | Passenger count (1–9) |
| `cls` | `string` | Cabin class string sent to the API |
| `showCls` | `boolean` | Whether the class dropdown is open |

---

### 5.2 Search Results `/search`

**File:** `src/app/search/page.tsx`

Reads search parameters from the URL query string, calls the backend, and renders the results.

#### URL Parameters (all set by the home page search form)

| Param | Example | Required |
|---|---|---|
| `origin` | `JFK` | Yes |
| `destination` | `LHR` | Yes |
| `departure_date` | `2026-08-01` | Yes |
| `return_date` | `2026-08-15` | No |
| `adults` | `2` | No (default 1) |
| `travel_class` | `BUSINESS` | No (default ECONOMY) |

#### Behaviour

1. On mount, calls `searchFlights()` from `src/lib/api.ts`.
2. While loading, renders 3 skeleton cards (grey shimmer rectangles).
3. On success, renders a `FlightCard` for each result with a staggered `animation-delay` (80ms per card) so they appear one after another.
4. If the backend returns an error, shows an error panel with a "Try again" link that re-triggers the search.
5. If results are empty after filtering, shows a "No flights found" empty state.

#### Controls

**Stop filter (pill buttons above results)**
- All / Nonstop / 1 stop
- Filters the local `offers` array client-side (no new API call)

**Sort buttons**
- Price (low → high) — default
- Price (high → low)
- Fewest stops

Sorting and filtering are applied together on every render via a derived `sorted` array.

**Source badge**
- A small dot below the controls indicates whether results came from live Amadeus data (green dot) or sample data (amber dot).

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `result` | `SearchResult \| null` | Full API response |
| `loading` | `boolean` | Shows skeleton cards |
| `error` | `string` | Shows error panel |
| `sort` | `string` | Active sort key |
| `stopFilter` | `number \| null` | `null` = all, `0` = nonstop, `1` = one stop |

---

### 5.3 Booking Flow `/booking`

**File:** `src/app/booking/page.tsx`

A 4-step linear wizard. All flight details are passed in as URL parameters from `FlightCard`.

#### URL Parameters (all set by FlightCard's "Select" button)

| Param | Example |
|---|---|
| `offer_id` | `SAMPLE-1` |
| `origin` | `JFK` |
| `destination` | `LHR` |
| `departure_date` | `2026-08-01` |
| `price_total` | `245.30` |
| `currency` | `USD` |
| `airline` | `American Airlines` |
| `flight_number` | `AA100` |
| `depart_time` | `08:30` |
| `arrive_time` | `11:45` |
| `cabin` | `ECONOMY` |

#### Step Indicator

Four steps are rendered as numbered circles connected by gradient lines. A completed step shows a green `CheckCircle` icon. The active step glows indigo. Future steps are grey.

```
①  ──────  ②  ──────  ③  ──────  ④
Passenger   Seat     Baggage   Confirm
```

#### Step 0 — Passenger Details

- **Full Name** text input
- **Email** email input
- Continue button is disabled until both fields are filled
- Calls no API — just collects data for Step 3

#### Step 1 — Seat Selection

- Renders the `SeatMap` component, fetching seat pricing from `GET /api/seat-map/{offer_id}` on page load.
- When the user clicks a seat, `seatType`, `seatNum`, and `seatFee` are stored in state.
- "Skip" button proceeds without selecting a seat (no seat API call will be made).
- "Confirm {seat}" button proceeds with the selection stored; the API call happens later in Step 3.

#### Step 2 — Baggage

Four baggage options displayed as toggleable cards:

| Option | Key | Fee |
|---|---|---|
| 23 kg Checked Bag | `CHECKED_23KG` | $35 |
| 32 kg Checked Bag | `CHECKED_32KG` | $60 |
| Extra Carry-On | `EXTRA_CARRYON` | $25 |
| Sports Equipment | `SPORTS_EQUIPMENT` | $70 |

Multiple options can be selected simultaneously. Each selected card highlights with an indigo border and shows a `CheckCircle` icon. The baggage state is an array of `{ key, qty }` objects.

#### Step 3 — Review & Confirm

Shows a summary of all chosen items with a line-by-line breakdown:

```
Passenger      John Doe
Email          john@example.com
Route          JFK → LHR
Date           2026-08-01
Flight         AA100 (ECONOMY)
Seat           12A (WINDOW) +$18
Baggage        23 kg Checked Bag +$35
─────────────────────────────────
Total          $298.30 USD
```

The **grand total** is calculated as: `parseFloat(price_total) + seatFee + sum(baggage fees)`.

Clicking "Confirm Booking" triggers three sequential API calls:
1. `POST /api/book` — creates the booking and returns `booking_id` and `pnr`
2. `POST /api/booking/{id}/seat` — assigns the seat (only if a seat was chosen)
3. `POST /api/booking/{id}/baggage` — once per selected baggage type

On success, the booking is saved to `localStorage` (key: `aerolink_bookings`) and the step transitions to the success screen.

#### Success Screen (step 4)

- Large green `CheckCircle` icon
- Booking ID and PNR displayed in monospace font
- Full summary of the booked trip
- Button: "View My Trips" → navigates to `/manage`

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `step` | `0–4` | Controls which step panel is visible |
| `name`, `email` | `string` | Passenger details |
| `seatFees` | `Record<string, number>` | From the seat-map API response |
| `seatType`, `seatNum`, `seatFee` | `string / number` | Chosen seat details |
| `bags` | `Array<{key, qty}>` | Selected baggage items |
| `bookingId`, `pnr` | `string` | Returned by the booking API |
| `busy` | `boolean` | Disables the confirm button during API calls |
| `error` | `string` | Inline error message |

---

### 5.4 My Trips `/manage`

**File:** `src/app/manage/page.tsx`

Displays and manages the user's bookings. Bookings are remembered from `localStorage` and supplemented by a live API lookup.

#### Booking lookup form

At the top of the page, a text input lets users look up any booking by ID (useful if they booked on a different device or want to share a booking ID with someone). Submitting adds it to the displayed list and fetches the live record.

#### Booking cards

Each card shows:
- An avatar with the origin airport code initials
- Route (origin → destination)
- Booking ID in monospace
- Status badge (CONFIRMED = green, MODIFIED = blue, CANCELLED = red)

Clicking a card fetches the full booking record from `GET /api/booking/{id}` (cached after first load) and expands an accordion with:
- PNR, date, passenger name, email
- Total price
- Seat assignment (if any)
- Baggage items (if any)
- Action buttons: **Modify** and **Cancel**

#### Modify modal

A centred overlay with:
- New departure date picker
- New destination IATA code input
- Warning that a $75 change fee applies
- Calls `POST /api/booking/{id}/modify` on confirm

#### Cancel

A browser `confirm()` dialog warns about the 80% refund policy. On confirm, calls `POST /api/booking/{id}/cancel` and updates the status badge in-place.

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `stored` | `StoredBooking[]` | Booking summaries from `localStorage` |
| `bookings` | `Record<string, Booking>` | Full records fetched from API, keyed by ID |
| `expanded` | `string \| null` | Currently expanded card's booking ID |
| `loading` | `Record<string, boolean>` | Per-card loading spinner |
| `modModal` | `string \| null` | Booking ID whose modify modal is open |
| `newDate`, `newDest` | `string` | Modify form values |

---

### 5.5 Price Tracker `/tracker`

**File:** `src/app/tracker/page.tsx`

Combines three backend features in one screen: price alert creation, historical fare visualisation, and active watch status checking.

#### Create Alert form

Fields:
- **From** (IATA, auto-uppercased)
- **To** (IATA, auto-uppercased)
- **Departure Date**
- **Alert me below $** (number — the target price threshold)
- **Your email** (where the alert will be sent)

Two actions:
- **View History** button — calls `GET /api/price/history` and shows the bar chart below
- **Create Alert** button — calls `POST /api/price/track` and adds the watch to the list below

#### Historical Fare Bar Chart

Five horizontal progress bars rendered with CSS `width` set to a percentage between min and max:

| Bar | Colour | What it represents |
|---|---|---|
| Cheapest seen | Emerald | Historical minimum price |
| Lower quarter | Teal | 25th percentile |
| Median | Indigo | 50th percentile |
| Upper quarter | Amber | 75th percentile |
| Most expensive | Red | Historical maximum |

Below the bars, a contextual tip compares the user's target price to the distribution:
- Below lower quarter → "an excellent deal"
- Below median → "a good deal"
- Above median → "try going lower"

#### Active Alerts list

Each watch card shows:
- Route (e.g. `JFK-LHR`)
- Watch ID in monospace
- Target price and email
- A refresh button (↻) that calls `GET /api/price/watch/{id}` and shows the current price inline
- A status pill: green if triggered ("Alert triggered! 🎉"), grey if still watching

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `watches` | `Watch[]` | Created alert records (session only, not persisted) |
| `checks` | `Record<string, PriceWatchCheck>` | Latest check results per watch ID |
| `history` | `PriceHistory \| null` | Fare distribution from the API |
| `busy`, `loadingHist` | `boolean` | Loading states for form submit and history fetch |

---

### 5.6 AI Assistant `/chat`

**File:** `src/app/chat/page.tsx`

A full-screen chat interface that connects directly to the Python multi-agent supervisor graph. The user types anything in natural language and the AI routes the request to the correct specialist agent.

#### Layout

Unlike the other pages, the chat page uses a flex-column full-height layout (`h-screen`) divided into three parts:
1. **Header bar** — Bot avatar, "AeroLink AI" label, live status dot, "Powered by OpenRouter" badge
2. **Message area** — scrollable, with `overflow-y: auto`; auto-scrolls to the bottom on each new message
3. **Input area** — fixed at the bottom; auto-expanding `<textarea>` + send button

#### Empty state

When no messages exist, the page shows:
- A large indigo plane icon with glow
- Headline: "How can I help you today?"
- 4 quick prompt cards (clickable, sends the prompt immediately):
  - "Find flights from JFK to LHR on 2026-08-01"
  - "What is the weather like in Tokyo?"
  - "Track the price for NYC to London under $250"
  - "What travel tips do you have for Paris?"

#### Message bubbles

- **User messages** — right-aligned, indigo tinted background, `User` icon avatar
- **AI messages** — left-aligned, glassmorphism background, `Bot` icon avatar
- **Agent label** — above each AI bubble, a colour-coded label shows which specialist responded:

| Agent | Label | Colour |
|---|---|---|
| `search_agent` | Search Agent | Sky blue |
| `booking_agent` | Booking Agent | Emerald |
| `ancillary_agent` | Ancillary Agent | Purple |
| `price_agent` | Price Agent | Amber |
| `info_agent` | Info Agent | Teal |

#### Typing indicator

While the API call is in flight, three animated dots pulse in sequence (staggered 200ms CSS animation) inside a glass bubble with the Bot avatar.

#### Input behaviour

- `Enter` sends the message
- `Shift+Enter` inserts a newline
- The `<textarea>` auto-grows vertically (up to 120px) as the user types, then scrolls internally
- The send button is disabled when the input is empty or a request is in flight

#### Conversation history

The full conversation is kept in a `messages` array in React state. On each send, the entire history is passed to `POST /api/chat` so the backend agent can see prior context. The history persists for the session but is lost on page refresh (no server-side session storage).

#### Local state

| Variable | Type | Purpose |
|---|---|---|
| `messages` | `Message[]` | Full conversation history |
| `input` | `string` | Current textarea value |
| `loading` | `boolean` | Controls typing indicator visibility |
| `error` | `string` | Inline error if API call fails |

---

## 6. Components

### 6.1 Navbar

**File:** `src/components/Navbar.tsx`

Fixed at the top of every page (`position: fixed; top: 0; z-index: 50`). Uses `glass-dark` styling with a subtle bottom border.

**Left side:** AeroLink logo — a gradient-background plane icon + "AeroLink" text in `gradient-text`.

**Right side (desktop, `md:` breakpoint and above):** Four navigation links with icons:

| Link | Route | Icon |
|---|---|---|
| Search | `/` | `Plane` |
| My Trips | `/manage` | `Briefcase` |
| Price Tracker | `/tracker` | `TrendingDown` |
| AI Assistant | `/chat` | `MessageSquare` |

The active link (matched via `usePathname()`) is highlighted with an indigo background pill and bordered in indigo.

**Right side (mobile):** The text labels are hidden; only the icons are shown, still with active state highlighting.

---

### 6.2 FlightCard

**File:** `src/components/FlightCard.tsx`

A horizontally laid-out card representing a single flight offer. Used on the `/search` page.

**Left airline strip** (gradient background, colour varies by airline code):

| Code | Gradient |
|---|---|
| AA | Blue |
| DL | Red |
| UA | Sky |
| BA | Indigo/Blue |
| EK | Red/Rose |
| LH | Yellow/Amber |
| Other | Indigo/Purple |

Contains the 2-letter airline code, flight number, and full airline name.

**Centre — flight info:**
- Departure time (left) and origin IATA code
- Animated route line with a centred plane icon
- Flight duration (calculated from depart/arrive times)
- Stop count badge — nonstop in emerald, any stops in amber
- Arrival time (right) and destination IATA code

**Right — meta + CTA:**
- Cabin class
- Low seats warning badge (shown only if `seats_remaining ≤ 5`)
- Price in `gradient-text-gold` (large font)
- "Select" button with a `ChevronRight` that shifts right on hover

**On click:** Builds a URL with all flight details as query parameters and navigates to `/booking`.

**Helper functions (internal):**
- `formatTime(t)` — converts ISO datetime strings or `HH:MM` strings to `HH:MM` display format using `toLocaleTimeString`
- `calcDuration(dep, arr)` — computes duration in hours and minutes from two time strings

---

### 6.3 SeatMap

**File:** `src/components/SeatMap.tsx`

An interactive visual representation of the aircraft cabin. Used in Step 1 of the booking flow.

#### Grid dimensions

- 30 rows × 6 columns
- Column layout: A B C | aisle gap | D E F
- Column headers (A–F) and row numbers (1–30) are displayed outside the grid

#### Seat type assignment rules

| Condition | Seat Type | API Key |
|---|---|---|
| Row 1, 2, or 3 | Bulkhead | `BULKHEAD` |
| Row 12 or 20 | Exit Row | `EXIT_ROW` |
| Column A or F | Window | `WINDOW` |
| Column B or E | Middle | `MIDDLE` |
| Column C or D | Aisle | `AISLE` |

Bulkhead and exit row rules take priority over column rules.

#### Pre-occupied seats (demo)

A fixed set of 27 seat IDs are hardcoded as occupied:
```
1A, 2F, 3B, 5C, 5D, 7A, 8F, 9B, 10E, 11C, 13A, 14D, 15F, 16B, 17C,
18E, 19A, 21F, 22C, 23B, 24D, 25A, 26F, 27C, 28B, 29E, 30A, 30F
```

In a production build, occupied seats would come from a real seat availability API.

#### Visual styles (CSS classes)

| State | Class | Colour |
|---|---|---|
| Window | `seat-window` | Blue (rgba 59,130,246) |
| Middle | `seat-middle` | Slate (rgba 100,116,139) |
| Aisle | `seat-aisle` | Purple (rgba 139,92,246) |
| Exit Row | `seat-exit` | Amber (rgba 245,158,11) |
| Bulkhead | `seat-bulkhead` | Pink (rgba 236,72,153) |
| Occupied | `seat-occupied` | Grey (not clickable, `cursor: not-allowed`) |
| Selected | `seat-selected` | Emerald (with glow box-shadow) |

Available seats scale to 115% on hover.

#### Legend

Above the grid, a colour-coded legend displays each seat type with its fee pulled from the live `seatFees` prop (falls back to hardcoded defaults if the API hasn't responded yet).

#### Props

```typescript
interface Props {
  fees: Record<string, number>;   // From GET /api/seat-map/{offer_id}
  onSelect: (seatType: string, seatNumber: string, fee: number) => void;
}
```

`onSelect` is called every time the user clicks an available seat. The booking page stores the returned values in state and uses them in Step 3 to call the seat assignment API.

---

## 7. Design System

### 7.1 Colour Palette

#### Custom Tailwind tokens (`tailwind.config.js`)

| Token | Hex | Usage |
|---|---|---|
| `space` | `#0a0f1e` | Main page background |
| `navy` | `#0d1427` | Alternate section background |
| `surface` | `#111827` | Card backgrounds |
| `surface-2` | `#1f2937` | Secondary card surfaces |
| `surface-3` | `#374151` | Borders, dividers |

#### CSS custom properties (`globals.css`)

| Variable | Value | Usage |
|---|---|---|
| `--glow-primary` | `rgba(99, 102, 241, 0.4)` | Indigo glow effects |
| `--glow-accent` | `rgba(245, 158, 11, 0.4)` | Gold glow effects |

#### Semantic colours used via Tailwind utilities

| Purpose | Tailwind class | Visual |
|---|---|---|
| Primary action / active | `indigo-500`, `indigo-600` | #6366f1 / #4f46e5 |
| Secondary / gradient | `purple-600`, `violet-700` | #9333ea / #7c3aed |
| Price / highlight | `amber-400`, `amber-500` | #fbbf24 / #f59e0b |
| Success / nonstop | `emerald-400`, `emerald-500` | #34d399 / #10b981 |
| Warning / stops | `amber-400` | #fbbf24 |
| Error | `red-400`, `red-500` | #f87171 / #ef4444 |
| Body text | `gray-200`, `gray-300` | Off-white |
| Muted text | `gray-400`, `gray-500` | Mid-grey |
| Disabled / subtle | `gray-600`, `gray-700` | Dark grey |

---

### 7.2 Typography

- **Font family:** System font stack — `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`
- **Anti-aliasing:** `-webkit-font-smoothing: antialiased`
- **Sizes:** Tailwind defaults (`text-xs` through `text-7xl`)
- **Display headings:** `font-black` (weight 900) for hero and large numbers
- **Section headings:** `font-bold` (weight 700)
- **Body / labels:** `font-medium` (weight 500) and `font-normal`
- **Monospace:** Used for booking IDs, PNR codes, watch IDs (`font-mono`)

---

### 7.3 CSS Utility Classes

All custom classes are defined in `src/app/globals.css`. They can be used alongside Tailwind utilities in any component.

#### Layout & surface

| Class | Effect |
|---|---|
| `.glass` | `background: rgba(255,255,255,0.04)` + `backdrop-filter: blur(20px)` + subtle border |
| `.glass-dark` | Like `.glass` but with `background: rgba(0,0,0,0.3)` — used for the Navbar and chat input |
| `.aurora-bg` | 300%-wide indigo→violet gradient with `aurora` CSS animation — used on the hero |

#### Typography

| Class | Effect |
|---|---|
| `.gradient-text` | Indigo → violet → pink clipped text gradient |
| `.gradient-text-gold` | Amber → gold → dark-amber clipped text gradient — used for prices |

#### Interactive elements

| Class | Effect |
|---|---|
| `.btn-primary` | Indigo→violet gradient button; brightens and lifts 1px on hover; glow shadow on hover; pseudo-element for smooth colour transition |
| `.btn-accent` | Amber gradient button |
| `.input-dark` | Semi-transparent dark input with indigo focus ring |
| `.card-hover` | `translateY(-4px)` + indigo border + drop shadow on hover |

#### Glows

| Class | Effect |
|---|---|
| `.glow-primary` | `box-shadow: 0 0 30px var(--glow-primary)` |
| `.glow-accent` | `box-shadow: 0 0 30px var(--glow-accent)` |
| `.glow-sm` | `box-shadow: 0 0 12px var(--glow-primary)` |

#### Feedback & loading

| Class | Effect |
|---|---|
| `.skeleton` | Horizontal shimmer gradient — for loading placeholder cards |
| `.typing-dot` | Used in threes inside a flex container; each dot pulses with a staggered 200ms delay |

#### Seat map

| Class | Effect |
|---|---|
| `.seat` | Base 28×28px seat shape (rounded top corners, airline-seat silhouette) |
| `.seat-window` | Blue tint |
| `.seat-middle` | Slate tint |
| `.seat-aisle` | Purple tint |
| `.seat-exit` | Amber tint |
| `.seat-bulkhead` | Pink tint |
| `.seat-occupied` | Grey, `cursor: not-allowed` |
| `.seat-selected` | Emerald with glow box-shadow |

#### Booking flow

| Class | Effect |
|---|---|
| `.step-line` | 2px horizontal line between step circles; grey by default |
| `.step-line.active` | Indigo→violet gradient — marks completed segments |

---

### 7.4 Animations & Motion

All animations are defined as Tailwind `keyframes` extensions and CSS `@keyframes` in `globals.css`. No JavaScript animation library is used.

| Name | Tailwind class | Duration | Effect |
|---|---|---|---|
| `aurora` | `animate-aurora` | 8s infinite | Shifts background-position 0%→100%→0% on the hero gradient |
| `float` | `animate-float` | 6s infinite | `translateY(0→-24px→0)` with a constant `-8deg` rotation — hero plane |
| `slideUp` | `animate-slide-up` | 0.5s forwards | Fades in + moves up 20px — used on hero text, flight cards |
| `fadeIn` | `animate-fade-in` | 0.4s forwards | Simple opacity 0→1 — used on expanded booking details, seat confirmation |
| `shimmer` | `animate-shimmer` | 1.5s infinite | Moves a highlight stripe left→right — skeleton loading cards |
| `typing-dot` | `.typing-dot` (CSS) | 1.2s infinite | Scale 0.6→1→0.6 with opacity — AI typing indicator |

Flight result cards use a staggered `animation-delay` (set inline via `style={{ animationDelay: '${i * 80}ms' }}`) so they cascade in sequentially rather than all appearing at once.

---

## 8. API Client (`src/lib/api.ts`)

All HTTP communication with the FastAPI backend is centralised in a single file. No page or component imports `fetch` directly.

### 8.1 TypeScript Types

```typescript
FlightOffer        // A single flight option from a search
SearchResult       // { source: string, offers: FlightOffer[] }
Booking            // Full booking record including seat and baggage
SeatMap            // { offer_id, currency, seat_types: Record<string, number> }
PriceHistory       // Route + five price quartiles
PriceWatch         // { watch_id, status, message }
PriceWatchCheck    // { watch_id, route, current_price, target_price, alert_triggered, action }
ChatResponse       // { response: string, agent: string }
SearchParams       // Input shape for searchFlights()
BookParams         // Input shape for bookFlight()
```

### 8.2 Available Functions

All functions return a `Promise<T>` where `T` is the corresponding TypeScript type above. Every function throws an `Error` with the backend's error message if the HTTP response is not OK.

#### Chat

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `sendChat(message, history)` | POST | `/api/chat` | Sends a message and conversation history to the multi-agent graph; returns the AI response and which agent handled it |

#### Search

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `searchFlights(params)` | POST | `/api/search` | Searches for flights; `params` includes origin, destination, date, adults, class, max results |
| `getAirportCode(city)` | GET | `/api/airport-code?city=` | Resolves a city name to IATA code(s) |

#### Booking

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `bookFlight(params)` | POST | `/api/book` | Creates a confirmed booking; returns `booking_id` and `pnr` |
| `getBooking(id)` | GET | `/api/booking/{id}` | Retrieves full booking record |
| `cancelBooking(id, reason?)` | POST | `/api/booking/{id}/cancel` | Cancels booking; returns refund estimate |
| `modifyBooking(id, params)` | POST | `/api/booking/{id}/modify` | Changes date or destination; returns `$75` change fee |

#### Ancillary

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `getSeatMap(offerId)` | GET | `/api/seat-map/{offerId}` | Returns seat types and their fees for an offer |
| `selectSeat(bookingId, seatType, seatNumber?)` | POST | `/api/booking/{id}/seat` | Assigns a seat to a booking |
| `addBaggage(bookingId, baggageType, quantity)` | POST | `/api/booking/{id}/baggage` | Adds a baggage item to a booking |

#### Price

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `trackPrice(params)` | POST | `/api/price/track` | Creates a price-drop alert; returns `watch_id` |
| `checkPriceWatch(watchId)` | GET | `/api/price/watch/{id}` | Returns current fare and whether the alert triggered |
| `getPriceHistory(origin, destination, date)` | GET | `/api/price/history` | Returns five fare quartiles for a route |

#### Info

| Function | Method | Endpoint | Description |
|---|---|---|---|
| `getWeather(city)` | GET | `/api/weather/{city}` | Current temperature, conditions, humidity |
| `getDestinationInfo(city)` | GET | `/api/destination/{city}` | Country, currency, timezone, language, travel tip |

---

## 9. Backend Connection

The frontend **never calls the Python server directly on port 8000**. Instead, all `fetch` calls go to `/api/...` (same origin, port 3000). Next.js rewrites these at the edge to `http://localhost:8000/api/...`:

```javascript
// next.config.js
async rewrites() {
  return [{
    source: '/api/:path*',
    destination: 'http://localhost:8000/api/:path*',
  }];
}
```

This means:
- No CORS issues in the browser
- The backend URL is a single configuration point — change it only in `next.config.js`
- The same rewrite works identically in development (`npm run dev`) and production (`npm run start`)

The FastAPI server itself also has CORS configured to allow `http://localhost:3000` as a fallback for non-Next.js clients (like Postman or curl).

---

## 10. State Management

The frontend has **no global state manager**. All state is local to each page component using React's `useState` hook. This is sufficient because:

- Pages don't share live data with each other
- The only cross-page data is the booking list, which uses `localStorage` (see §11)
- The chat history lives in the `/chat` page only, for the duration of the session

Each page is self-contained and fetches its own data from the backend on mount.

---

## 11. Local Persistence

Only one piece of data is stored in the browser's `localStorage`:

**Key:** `aerolink_bookings`
**Format:** JSON array of `StoredBooking` objects, newest first, capped at 20 entries
**Written by:** `/booking` page after a successful booking confirmation
**Read by:** `/manage` page on mount to populate the booking list

```typescript
interface StoredBooking {
  booking_id: string;
  pnr: string;
  origin: string;
  destination: string;
  departure_date: string;
  airline: string;
  flight_number: string;
}
```

This is a lightweight summary only. Full booking details (seat, baggage, status) are always fetched live from the API to ensure they reflect the current server state.

---

## 12. Configuration Files

### `next.config.js`

```javascript
async rewrites() {
  return [{ source: '/api/:path*', destination: 'http://localhost:8000/api/:path*' }];
}
```

The only configuration needed — routes `/api/*` calls to the Python backend.

### `tailwind.config.js`

Extends Tailwind with:
- 5 custom colour tokens (`space`, `navy`, `surface`, `surface-2`, `surface-3`)
- 6 custom animations (`aurora`, `float`, `slide-up`, `fade-in`, `shimmer`, `pulse2`)
- 5 keyframe definitions
- `backgroundSize: { '300%': '300%' }` — required for the aurora animation

### `tsconfig.json`

Standard Next.js TypeScript config with one key addition:
```json
"paths": { "@/*": ["./src/*"] }
```
This enables `@/components/...`, `@/lib/...` imports instead of relative paths.

### `postcss.config.js`

Standard: `tailwindcss` + `autoprefixer` plugins only.

---

## 13. Build Output

Running `npm run build` produces 6 static routes:

```
Route (app)        Size     First Load JS
/                  4.61 kB      91.7 kB
/booking           6.14 kB      93.3 kB
/chat              3.78 kB      90.9 kB
/manage            4.52 kB      91.7 kB
/search            4.86 kB      92.0 kB
/tracker           3.90 kB      91.0 kB
```

Shared JS chunk: **87.1 kB** (React runtime + Tailwind). Each individual page adds 3–6 kB of page-specific code. Total first-load JS per page is under 95 kB — well within performance budget.

All routes are pre-rendered as static HTML (`○ Static`) since they contain no server-side data fetching. All data is fetched client-side after hydration.

---

## 14. Extending the Frontend

### Adding a new page

1. Create `src/app/<name>/page.tsx` with `'use client';` at the top
2. Add it to the `links` array in `src/components/Navbar.tsx`
3. Add any new API calls to `src/lib/api.ts`

### Adding a new API function

In `src/lib/api.ts`:
```typescript
export function myNewFunction(param: string) {
  return request<MyReturnType>(`/my-endpoint?param=${param}`);
}
```
The `request` helper handles authentication headers, JSON parsing, and error extraction automatically.

### Changing the backend URL

For deployment, change only one line in `next.config.js`:
```javascript
destination: 'https://your-api-server.com/api/:path*'
```

### Adding real images to destination cards

The destination cards in `/` currently use emoji + gradient backgrounds. To add real images, replace the gradient `<button>` with a `next/image` `<Image>` component and add the image domain to `next.config.js`:
```javascript
images: { domains: ['your-cdn.com'] }
```

### Persisting chat history

Currently the chat history resets on page refresh. To persist it, write `messages` to `localStorage` on every update:
```typescript
useEffect(() => {
  localStorage.setItem('chat_history', JSON.stringify(messages));
}, [messages]);
```
And load it on mount with `useState(() => JSON.parse(localStorage.getItem('chat_history') ?? '[]'))`.
