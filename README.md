# AeroLink — AI Flight Multi-Agent System

A full-stack flight assistant powered by a **supervisor + specialist multi-agent architecture**.
Users can search, book, and manage flights through either a modern web UI or a terminal CLI.
The backend is built with LangChain, LangGraph, and LangSmith; the frontend with Next.js 14.

---

## What It Does

Type a single natural-language request — the system breaks it into sub-tasks and routes each
one to the right specialist AI agent automatically:

> *"Find a flight from JFK to LHR on 2026-08-01, book the cheapest one for John Doe,
> add a window seat and a checked bag, and tell me the weather in London."*

One message → four agents → one coherent reply.

---

## Repository Layout

```
flight_multi_agent/
│
├── backend/                        ← Python multi-agent system + REST API
│   ├── requirements.txt            # Python dependencies
│   ├── .env.example                # Environment variable template
│   ├── src/
│   │   ├── api.py                  # FastAPI server (15 REST endpoints)
│   │   ├── graph.py                # LangGraph supervisor + routing
│   │   ├── state.py                # Shared AgentState TypedDict
│   │   ├── llm.py                  # OpenRouter LLM factory (ChatOpenAI)
│   │   ├── config.py               # Env loading + LangSmith tracing
│   │   ├── main.py                 # CLI entry point
│   │   ├── agents/
│   │   │   └── specialists.py      # Five ReAct specialist agents
│   │   └── tools/
│   │       ├── search_tools.py     # search_flights, get_airport_code
│   │       ├── booking_tools.py    # book_flight, get_booking, cancel, modify
│   │       ├── ancillary_tools.py  # get_seat_map, select_seat, add_baggage
│   │       ├── price_tools.py      # track_price, check_price_watch, price_history
│   │       ├── weather_tools.py    # get_destination_weather, get_destination_info
│   │       └── store.py            # In-memory booking + watch store
│   └── tests/
│       └── test_tools.py           # Tool-layer smoke tests (no LLM needed)
│
├── frontend/                       ← Next.js 14 web UI
│   ├── next.config.js              # Proxies /api/* → localhost:8000
│   ├── tailwind.config.js
│   ├── package.json
│   └── src/
│       ├── app/
│       │   ├── page.tsx            # Home — animated hero + search form
│       │   ├── search/page.tsx     # Flight results with sort & filter
│       │   ├── booking/page.tsx    # 4-step booking wizard + seat map
│       │   ├── manage/page.tsx     # My Trips — view, modify, cancel
│       │   ├── tracker/page.tsx    # Price alerts + fare history
│       │   └── chat/page.tsx       # Live AI chat with the multi-agent graph
│       ├── components/
│       │   ├── Navbar.tsx
│       │   ├── FlightCard.tsx
│       │   └── SeatMap.tsx         # Interactive 30-row × 6-col cabin grid
│       └── lib/
│           └── api.ts              # Typed fetch wrappers for all 15 endpoints
│
├── README.md                       ← This file
├── FUNCTIONAL_SPEC.md              # Full system walkthrough (laymen + technical)
└── Frontend_spec.md                # Frontend design system, pages, components
```

---

## Architecture

```
                         ┌─────────────────────┐
              User ────► │     SUPERVISOR       │ ◄───────────────────┐
           (UI / CLI)    │  (LLM router via     │                     │
                         │   structured output) │                     │
                         └──────────┬──────────┘                     │
                                    │ routes to one specialist         │ reports back
                   ┌────────────────┼──────────────────┐             │
                   ▼                ▼                   ▼             │
            ┌────────────┐  ┌─────────────┐  ┌────────────────┐     │
            │search_agent│  │booking_agent│  │ancillary_agent │     │
            └────────────┘  └─────────────┘  └────────────────┘     │
            search_flights   book_flight       get_seat_map           │
            get_airport_code get_booking       select_seat            │
                             cancel_booking    add_baggage            │
                             modify_booking                           │
                   ┌──────────────────┐   ┌──────────────────┐       │
                   │   price_agent    │   │   info_agent     │ ──────┘
                   └──────────────────┘   └──────────────────┘
                   track_price             get_destination_weather
                   check_price_watch       get_destination_info
                   get_price_history
```

---

## Specialist Agents

| Agent | What it does | Tools |
|---|---|---|
| `search_agent` | Find flights & resolve airport codes | `search_flights`, `get_airport_code` |
| `booking_agent` | Create, view, cancel, modify reservations | `book_flight`, `get_booking`, `cancel_booking`, `modify_booking` |
| `ancillary_agent` | Seat selection & baggage add-ons | `get_seat_map`, `select_seat`, `add_baggage` |
| `price_agent` | Price alerts, fare drops, price history | `track_price`, `check_price_watch`, `get_price_history` |
| `info_agent` | Destination weather & travel tips | `get_destination_weather`, `get_destination_info` |

---

## Quick Start

You can run either the Python backend (`backend/`) or the Java Spring Boot backend
(`springboot-backend-api/`). Both expose the same 15 REST endpoints, so the Next.js
frontend works against either one with zero changes.

### Option A — Docker Compose (full stack: PostgreSQL + Java backend + Next.js)

```bash
# From the repo root
cp backend/.env.example .env    # or create a .env with your OPENROUTER_API_KEY
docker compose up --build
```

Once the containers are healthy:

| Service     | URL                       |
|-------------|---------------------------|
| Frontend    | http://localhost:3000     |
| Java API    | http://localhost:8000/api |
| PostgreSQL  | localhost:5432 (flightdb) |

`docker compose down -v` tears everything down and wipes the database volume.

### Option B — Manual (pick a backend, plus the frontend)

Open two terminal tabs.

### 1 — Backend (Python / FastAPI)

```bash
cd backend

# Create and activate a virtual environment
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Set up environment variables
cp .env.example .env               # then open .env and fill in your keys
```

**Minimum required key:**

```env
OPENROUTER_API_KEY=sk-or-your-key-here
```

**Start the API server:**

```bash
uvicorn src.api:app --reload --port 8000
```

The API will be available at `http://localhost:8000`.

### 1b — Backend (Java / Spring Boot, alternative)

A drop-in Java replacement for the Python backend lives in `springboot-backend-api/`.
It exposes the same 15 endpoints (Spring AI 1.0 + OpenRouter, Hibernate/JPA + H2 in
dev, PostgreSQL in prod). Requires Java 21 and Maven.

```bash
cd springboot-backend-api

# Set keys (same names as the Python backend)
export OPENROUTER_API_KEY=sk-or-your-key-here

# Run on :8000
mvn spring-boot:run
```

Run tests (no LLM, no network, no keys needed):

```bash
mvn test
```

### 2 — Frontend (Next.js)

In a separate terminal:

```bash
cd frontend
npm install                        # only needed once
npm run dev                        # starts on http://localhost:3000
```

Open **http://localhost:3000** in any browser.

### 3 — CLI (optional, no frontend needed)

From the `backend/` directory with the venv active:

```bash
# One-shot query
python -m src.main "Find flights from JFK to LHR on 2026-08-01"

# Interactive chat session
python -m src.main
```

---

## Environment Variables

All variables are read from `backend/.env`. Copy `backend/.env.example` to get started.

| Variable | Required | Description |
|---|---|---|
| `OPENROUTER_API_KEY` | **Yes** | API key from [openrouter.ai](https://openrouter.ai) — powers all AI agents |
| `OPENROUTER_MODEL` | No (default: `openrouter/auto`) | `openrouter/auto` enables OpenRouter's Auto Router (best model picked per query). Override with any slug from openrouter.ai/models |
| `LANGSMITH_API_KEY` | No | Enables distributed tracing in [LangSmith](https://smith.langchain.com) |
| `LANGSMITH_TRACING` | No | Set to `true` to activate tracing |
| `LANGSMITH_PROJECT` | No | Project name in LangSmith dashboard |
| `AMADEUS_CLIENT_ID` | No | [Amadeus Self-Service](https://developers.amadeus.com) — live flight search |
| `AMADEUS_CLIENT_SECRET` | No | Amadeus secret (paired with the above) |
| `AMADEUS_HOSTNAME` | No | `test` (sandbox) or `production` |
| `OPENWEATHER_API_KEY` | No | [OpenWeatherMap](https://openweathermap.org/api) — real weather data |

Without the optional keys the system runs fully on clearly-labelled **sample data**.

---

## REST API Endpoints

The FastAPI server (`backend/src/api.py`) exposes 15 endpoints used by the frontend.
Full interactive docs are available at `http://localhost:8000/docs` when the server is running.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat` | Send a message to the multi-agent graph |
| POST | `/api/search` | Search for available flights |
| GET | `/api/airport-code?city=` | Resolve a city name to IATA code |
| POST | `/api/book` | Create a flight booking |
| GET | `/api/booking/{id}` | Retrieve a booking by ID |
| POST | `/api/booking/{id}/cancel` | Cancel a booking |
| POST | `/api/booking/{id}/modify` | Change date or destination |
| GET | `/api/seat-map/{offer_id}` | Get seat types and fees for an offer |
| POST | `/api/booking/{id}/seat` | Assign a seat to a booking |
| POST | `/api/booking/{id}/baggage` | Add baggage to a booking |
| POST | `/api/price/track` | Create a price-drop alert |
| GET | `/api/price/watch/{id}` | Check current fare for an alert |
| GET | `/api/price/history` | Historical fare distribution for a route |
| GET | `/api/weather/{city}` | Current weather for a destination |
| GET | `/api/destination/{city}` | Travel info (currency, timezone, tips) |

---

## Frontend Pages

| URL | Page | Key features |
|---|---|---|
| `/` | Home | Animated aurora hero, flight search form, popular destinations |
| `/search` | Search Results | Flight cards, nonstop/stops filter, price/stop sort |
| `/booking` | Booking Wizard | Passenger → seat map → baggage → confirm (4 steps) |
| `/manage` | My Trips | Look up, expand, modify, or cancel bookings |
| `/tracker` | Price Tracker | Create alerts, view fare bar chart, check alert status |
| `/chat` | AI Assistant | Live chat with the multi-agent graph, per-agent colour labels |

---

## Running Tests

From the `backend/` directory with the venv active:

```bash
pip install pytest
python -m pytest tests/ -q
```

The test suite covers the tool layer only — no LLM calls, no network requests, no API keys needed.

| Test | Covers |
|---|---|
| `test_search_returns_offers` | `search_flights` returns at least one offer |
| `test_book_then_cancel` | Full booking + cancellation cycle; refund estimate |
| `test_seat_and_baggage` | Seat assignment fee + baggage fee calculation |
| `test_price_watch` | Price watch creation and check response shape |

---

## Documentation

| Document | Description |
|---|---|
| [`FUNCTIONAL_SPEC.md`](./FUNCTIONAL_SPEC.md) | Complete system walkthrough — architecture, all tools, end-to-end example, production roadmap |
| [`Frontend_spec.md`](./Frontend_spec.md) | Frontend design system, all pages and components, API client reference, extension guide |

---

## Going to Production

**Backend**
- Replace `backend/src/tools/store.py` (in-memory dict) with Postgres or DynamoDB.
- Fill the live-API `# TODO` stubs in `booking_tools.py` with Amadeus Flight Create Orders calls.
- Add Celery Beat or AWS EventBridge to run `check_price_watch` on a schedule and dispatch alert emails.
- Enable LangGraph checkpointing (`MemorySaver` or Postgres checkpointer) for persistent multi-session conversations.

**Frontend**
- Change the `destination` in `frontend/next.config.js` from `localhost:8000` to your deployed API URL.
- Add real destination images via `next/image` (add CDN domain to `next.config.js`).
- Persist chat history in `localStorage` or a backend session for continuity across page refreshes.

**Infrastructure**
- Run the FastAPI server behind a production ASGI server (Gunicorn + Uvicorn workers).
- Set `LANGSMITH_TRACING=true` and a valid `LANGSMITH_API_KEY` to get full observability in production.

## Instruction

To run the project going forward:
# Terminal 1 â API server
cd backend && uvicorn src.api:app --reload --port 8000

# Terminal 2 â Web UI
cd frontend && npm run dev

# Tests
cd backend && python -m pytest tests/ -q


# docker backend switch
./switch-backend-docker.sh spring   # bring up Spring Boot backend in compose
./switch-backend-docker.sh python   # swap to Python FastAPI (uses override file)
./switch-backend-docker.sh status   # ps + :8000 health check
./switch-backend-docker.sh stop     # stop backend, keep postgres/frontend
./switch-backend-docker.sh down     # tear down full stack

