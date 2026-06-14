# Flight Multi-Agent System — Functional Specification

## Table of Contents

1. [What This System Does (Plain English)](#1-what-this-system-does-plain-english)
2. [How It Works — The Big Picture](#2-how-it-works--the-big-picture)
3. [The Supervisor — The Traffic Controller](#3-the-supervisor--the-traffic-controller)
4. [The Five Specialist Agents](#4-the-five-specialist-agents)
5. [What Each Agent Can Do (Tools)](#5-what-each-agent-can-do-tools)
6. [A Full End-to-End Example](#6-a-full-end-to-end-example)
7. [Data Flow Diagram](#7-data-flow-diagram)
8. [Setting Up the System](#8-setting-up-the-system)
9. [Running the System](#9-running-the-system)
10. [Technical Architecture (For Developers)](#10-technical-architecture-for-developers)
11. [External Services & API Keys](#11-external-services--api-keys)
12. [Testing](#12-testing)
13. [Current Limitations & Production Roadmap](#13-current-limitations--production-roadmap)

---

## 1. What This System Does (Plain English)

Imagine calling a travel agency and being transferred between different specialists depending on what you need — a ticket agent, a seat-selection desk, a baggage office, a fare-deals team, and a destination concierge. This system does exactly that, but automatically and in software.

You type a single request in natural language — for example:

> *"Find a flight from New York to London next August, book the cheapest one for me, grab a window seat and a checked bag, and tell me what the weather will be like."*

The system reads your request, breaks it into sub-tasks, and routes each sub-task to the right AI specialist. Each specialist uses the appropriate tools (flight search APIs, booking systems, etc.) to get the job done. They all report back, and you receive a single, coherent answer.

**Key things the system can do:**

| What you want | Who handles it |
|---|---|
| Find available flights between two cities | Search Agent |
| Book, view, cancel, or change a reservation | Booking Agent |
| Pick a seat or add checked/special baggage | Ancillary Agent |
| Set a price alert or check fare history | Price Agent |
| Get weather or travel tips for a destination | Info Agent |

---

## 2. How It Works — The Big Picture

The system uses a pattern called **"supervisor + specialists"**. Think of it like a hospital: a triage nurse (the supervisor) assesses every patient and directs them to the right doctor (the specialist). The doctor treats the patient and sends them back to the nurse, who checks whether the patient is fully taken care of or needs another specialist.

```
                         ┌─────────────┐
              User ───►  │  SUPERVISOR │  ◄─────────────────────┐
                         └──────┬──────┘                        │
                                │ routes to one specialist       │ specialist reports back
                  ┌─────────────┼─────────────────┐             │
                  ▼             ▼                  ▼             │
           ┌────────────┐ ┌──────────────┐ ┌───────────────┐   │
           │search_agent│ │booking_agent │ │ancillary_agent│   │
           └────────────┘ └──────────────┘ └───────────────┘   │
                  ▼             ▼                  ▼             │
           uses flight    uses booking       uses seat &         │
           search API     store               baggage fees       │
                                                                 │
                  ┌──────────────┐  ┌──────────────┐            │
                  │  price_agent │  │  info_agent  │ ───────────┘
                  └──────────────┘  └──────────────┘
                  tracks fares &    weather &
                  price history     travel tips
```

This loop — supervisor → specialist → supervisor — continues until the supervisor decides the request has been fully answered, at which point it signals `FINISH` and the final answer is returned to you.

---

## 3. The Supervisor — The Traffic Controller

The supervisor is a special AI node that reads the entire conversation history and decides, at each step, which specialist should act next (or whether the job is done).

**How it makes decisions:**

- It has a fixed system prompt that describes every specialist and what they cover.
- It calls the language model (via OpenRouter) and asks it to return a structured routing decision — one of: `search_agent`, `booking_agent`, `ancillary_agent`, `price_agent`, `info_agent`, or `FINISH`.
- It never uses tools directly. Its only job is to read and route.

**Routing rules enforced in the prompt:**

- Route to exactly one specialist at a time (no parallel dispatch in this version).
- Only send `FINISH` when the user's entire request has been addressed.

---

## 4. The Five Specialist Agents

Each specialist is a self-contained AI agent built with the **ReAct** pattern (Reason + Act): it reasons about the task, decides which tool to call, calls it, reads the result, reasons again, and repeats until it has a complete answer to report back.

### 4.1 Search Agent (`search_agent`)

**Purpose:** Find available flights and resolve city/airport names.

**Personality prompt:** *"You are a flight search specialist. Present offers with their offer_id, times, stops, and price so the user (or another agent) can book them."*

Crucially, it preserves the `offer_id` in its response — the booking agent needs this identifier to create a reservation.

---

### 4.2 Booking Agent (`booking_agent`)

**Purpose:** Create new reservations, look up existing ones, cancel them, or change the date/destination.

**Personality prompt:** *"Confirm key details (passenger, route, date, price) before booking, and always report the booking_id and PNR."*

Every booking gets a unique `booking_id` (e.g. `BK-A3F29C1B`) and a PNR (Passenger Name Record) code. These are passed between agents so subsequent agents (seat selection, baggage) know which booking to modify.

---

### 4.3 Ancillary Agent (`ancillary_agent`)

**Purpose:** Handle everything that is bolted on to a booking after it is confirmed — seat assignment and baggage add-ons.

**Personality prompt:** *"Show the seat map / fees when helpful and confirm the booking_id before making changes."*

It first retrieves the seat map (types and prices) for an offer, then assigns the chosen seat or adds baggage items to an existing booking record.

---

### 4.4 Price Agent (`price_agent`)

**Purpose:** Advise on whether a fare is a good deal, set price-drop alerts, and check the status of those alerts.

**Personality prompt:** *"Create price watches, check them, and provide historical price context to advise on whether a fare is a good deal."*

A price watch stores a target price and an email address. When checked, it compares the current fare against the target and reports whether the alert has triggered.

---

### 4.5 Info Agent (`info_agent`)

**Purpose:** Provide destination-specific information to help the traveler plan their trip.

**Personality prompt:** *"Provide destination weather and practical travel information to help the traveler plan their trip."*

It fetches current weather conditions (temperature, sky conditions, humidity) and practical travel facts (local currency, timezone, language, tips).

---

## 5. What Each Agent Can Do (Tools)

Tools are callable functions the AI agents use to take concrete actions. Each agent has access only to its own relevant tool subset.

### Search Tools

| Tool | What it does |
|---|---|
| `search_flights` | Searches for available flights between two airports on a given date. Returns a list of offers including airline, times, number of stops, cabin class, price, and seats remaining. |
| `get_airport_code` | Converts a city or airport name (e.g. "London") to its IATA code (e.g. "LHR"). |

**Parameters for `search_flights`:** origin (IATA), destination (IATA), departure date, optional return date, number of adults, cabin class (ECONOMY / BUSINESS / FIRST), and max results.

---

### Booking Tools

| Tool | What it does |
|---|---|
| `book_flight` | Creates a confirmed reservation for a previously found offer. Returns a `booking_id` and PNR. |
| `get_booking` | Retrieves the full details of a booking by its `booking_id`. |
| `cancel_booking` | Cancels a booking and returns an estimated refund (80% of ticket price in the demo). |
| `modify_booking` | Changes the departure date or destination of an existing booking. A $75 change fee is applied. |

---

### Ancillary Tools

| Tool | What it does |
|---|---|
| `get_seat_map` | Returns the available seat types and their fees for a given flight offer. |
| `select_seat` | Assigns a specific seat type (WINDOW, AISLE, MIDDLE, EXIT_ROW, BULKHEAD) and optionally a seat number to a booking. |
| `add_baggage` | Adds one or more baggage items (23 kg checked bag, 32 kg checked bag, extra carry-on, sports equipment) to a booking. |

**Seat fees (demo):** MIDDLE $0 · AISLE $15 · WINDOW $18 · BULKHEAD $29 · EXIT_ROW $39

**Baggage fees (demo):** Extra carry-on $25 · Checked 23 kg $35 · Checked 32 kg $60 · Sports equipment $70

---

### Price Tools

| Tool | What it does |
|---|---|
| `track_price` | Creates a price-drop alert for a route. Stores a target price and an email; triggers when the fare reaches or falls below the target. Returns a `watch_id`. |
| `check_price_watch` | Checks the current fare for an active price watch and reports whether the alert has triggered. |
| `get_price_history` | Returns historical fare context (minimum, lower quartile, median, upper quartile, maximum) for a route, helping you judge whether a current offer is cheap or expensive. |

---

### Weather & Info Tools

| Tool | What it does |
|---|---|
| `get_destination_weather` | Returns current temperature (°C), sky conditions, and humidity for a city. Uses OpenWeatherMap if an API key is configured. |
| `get_destination_info` | Returns practical travel facts: country, local currency, timezone, language, and a travel tip. |

---

## 6. A Full End-to-End Example

**User input:**
> *"Find a flight from JFK to LHR on 2026-08-01, book the cheapest one for John Doe (john@example.com), add a window seat and one checked bag, then tell me the weather in London."*

**What happens internally:**

```
Step 1 — Supervisor reads request → routes to search_agent

  search_agent calls search_flights(JFK, LHR, 2026-08-01)
  → returns 2 offers; cheapest is SAMPLE-2 at $198.00
  → reports back with offer details including offer_id

Step 2 — Supervisor sees flight was found → routes to booking_agent

  booking_agent calls book_flight(offer_id=SAMPLE-2, "John Doe", ...)
  → booking confirmed: BK-9A3F1C2D, PNR: 9A3F1C2D

Step 3 — Supervisor sees booking created → routes to ancillary_agent

  ancillary_agent calls select_seat(BK-9A3F1C2D, WINDOW)
  → seat assigned, fee $18.00
  ancillary_agent calls add_baggage(BK-9A3F1C2D, CHECKED_23KG, 1)
  → baggage added, fee $35.00

Step 4 — Supervisor sees seat + bag done → routes to info_agent

  info_agent calls get_destination_weather("London")
  → 16°C, light rain, humidity 74%
  info_agent calls get_destination_info("London")
  → Currency: GBP, Timezone: GMT/BST, Tip: Get an Oyster card for the Tube.

Step 5 — Supervisor sees all tasks complete → FINISH

Final response to user:
  ✓ Found 2 flights; cheapest is DL205 at $198.00 (1 stop)
  ✓ Booked for John Doe — Booking ID: BK-9A3F1C2D, PNR: 9A3F1C2D
  ✓ Window seat assigned (+$18.00)
  ✓ One checked bag (23 kg) added (+$35.00)
  ✓ London weather: 16°C, light rain. Tip: Get an Oyster card.
```

---

## 7. Data Flow Diagram

```
User Input (natural language)
        │
        ▼
┌──────────────────────────────────────────────────┐
│                    GRAPH (LangGraph)              │
│                                                  │
│  ┌─────────────────────────────────────────┐     │
│  │             AgentState                  │     │
│  │  messages: [HumanMessage, AIMessage, …] │     │
│  │  next:     "search_agent" | "FINISH" …  │     │
│  └─────────────────────────────────────────┘     │
│              │ (state passed between nodes)       │
│              ▼                                   │
│  ┌────────────────────┐                          │
│  │     supervisor     │◄────────────┐            │
│  │  (LLM + structured │             │            │
│  │   output → Route)  │             │            │
│  └────────┬───────────┘             │            │
│           │ conditional edge        │            │
│           ▼                         │            │
│  ┌──────────────────┐               │            │
│  │ specialist node  │               │            │
│  │  (ReAct agent)   │ ──────────────┘            │
│  │  calls 1..N tools│   reports back as          │
│  └──────────────────┘   HumanMessage(name=agent) │
│                                                  │
└──────────────────────────────────────────────────┘
        │
        ▼
  Final Answer to User
```

**State object (`AgentState`):**
- `messages` — the full conversation list; each node appends to it (never overwrites). This means every agent can see what every previous agent said.
- `next` — the routing decision written by the supervisor; read by the conditional edge to dispatch to the correct specialist node.

---

## 8. Setting Up the System

### Prerequisites

- Python 3.10 or newer
- An [OpenRouter](https://openrouter.ai) account and API key

### Step-by-step

```bash
# 1. Clone the repository
git clone https://github.com/malayab365/flight-multi-agent.git
cd flight-multi-agent

# 2. Create and activate a virtual environment
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Create your .env file from the template
cp .env.example .env
```

Now open `.env` and fill in at minimum:

```env
# Required — your OpenRouter key
OPENROUTER_API_KEY=sk-or-your-key-here

# Optional — change the model (default is openai/gpt-4o)
# Any model slug from https://openrouter.ai/models works here
OPENROUTER_MODEL=openai/gpt-4o
```

Everything else in `.env` is optional. The system runs end-to-end on sample data without any other keys.

---

## 9. Running the System

### Interactive chat (recommended for exploration)

```bash
python -m src.main
```

You will see a prompt:

```
Flight assistant ready. Type 'exit' to quit.

you>
```

Type any flight-related request and press Enter. The conversation history is preserved across turns so you can refer to earlier results (e.g. "now cancel that booking").

### One-shot query

```bash
python -m src.main "Find flights from New York to Tokyo on 2026-09-15"
```

The result is printed and the program exits.

### Example queries to try

```
Find me a business class flight from Dubai to Paris on 2026-07-10

Book the second offer for Jane Smith, jane@example.com

Add a window seat and two checked bags to my booking

Set a price alert for JFK to LAX — email me at user@example.com if it drops below $120

What is the weather like in Tokyo? And what currency do they use?
```

---

## 10. Technical Architecture (For Developers)

### Project layout

```
flight_multi_agent/
├── .env.example              # Template for environment variables
├── .gitignore
├── README.md                 # Quick-start guide
├── FUNCTIONAL_SPEC.md        # This document
├── requirements.txt
├── src/
│   ├── __init__.py
│   ├── config.py             # Env var loading + LangSmith setup
│   ├── llm.py                # LLM factory (OpenRouter via ChatOpenAI)
│   ├── state.py              # AgentState TypedDict definition
│   ├── graph.py              # Supervisor graph; node wiring
│   ├── main.py               # CLI entry point
│   ├── agents/
│   │   ├── __init__.py
│   │   └── specialists.py    # ReAct agent builder per specialist
│   └── tools/
│       ├── __init__.py       # Exports tool lists
│       ├── store.py          # In-memory persistence (bookings, watches)
│       ├── search_tools.py   # search_flights, get_airport_code
│       ├── booking_tools.py  # book_flight, get_booking, cancel, modify
│       ├── ancillary_tools.py# get_seat_map, select_seat, add_baggage
│       ├── price_tools.py    # track_price, check_price_watch, get_price_history
│       └── weather_tools.py  # get_destination_weather, get_destination_info
└── tests/
    └── test_tools.py         # Smoke tests for the tool layer (no LLM needed)
```

### Key frameworks

| Library | Version | Role |
|---|---|---|
| `langchain` | ≥ 0.3 | Tool definitions, message types, model abstraction |
| `langchain-openai` | ≥ 0.2 | `ChatOpenAI` pointed at OpenRouter |
| `langgraph` | ≥ 0.2.50 | Stateful graph engine; supervisor + specialist nodes |
| `langsmith` | ≥ 0.1.140 | Distributed tracing of every LLM call and tool invocation |
| `pydantic` | ≥ 2.7 | Structured output schema for the supervisor's routing decision |
| `python-dotenv` | ≥ 1.0 | `.env` file loading |
| `requests` | ≥ 2.32 | HTTP calls to OpenWeatherMap |

### How the LLM is wired up

`src/llm.py` creates a cached `ChatOpenAI` instance pointed at `https://openrouter.ai/api/v1`. OpenRouter accepts OpenAI-compatible requests and proxies them to whichever model is specified in `OPENROUTER_MODEL`. The same instance is shared by the supervisor and all five specialist agents.

### How state flows through the graph

1. The user's message enters as `HumanMessage` in `AgentState.messages`.
2. The `supervisor` node reads all messages, calls the LLM with structured output (Pydantic `Route` model), and writes `next` into state.
3. The conditional edge `_route_from_supervisor` reads `state["next"]` and dispatches to the corresponding specialist node, or ends the graph if `FINISH`.
4. The specialist node invokes its ReAct agent. The agent's final message is extracted and re-injected into state as `HumanMessage(content=..., name="<agent_name>")` so the supervisor knows who spoke.
5. Every specialist node has an unconditional edge back to `supervisor`, restarting the cycle.

### How the supervisor makes routing decisions

The supervisor's LLM call uses `llm.with_structured_output(Route)` — this forces the model to return a JSON object matching the `Route` Pydantic schema (a single `next` field constrained to valid specialist names or `FINISH`). This prevents hallucinated or ambiguous routing decisions.

### Persistence

`src/tools/store.py` is a thread-safe in-memory dictionary. Bookings and price watches live here for the duration of the process. It is intentionally simple — the interface (`save_booking`, `get_booking`, `save_watch`) makes it straightforward to swap in a real database without touching any tool code.

### LangSmith tracing

Importing `src/config.py` (which happens automatically on every startup) sets the `LANGCHAIN_TRACING_V2`, `LANGCHAIN_API_KEY`, and `LANGSMITH_*` environment variables. LangChain and LangGraph pick these up at runtime, so every LLM call, tool invocation, and graph node transition is captured in LangSmith with zero additional code.

---

## 11. External Services & API Keys

| Key | Required | Service | What it unlocks |
|---|---|---|---|
| `OPENROUTER_API_KEY` | **Yes** | [OpenRouter](https://openrouter.ai) | The LLM that powers all agents |
| `OPENROUTER_MODEL` | No (default: `openai/gpt-4o`) | OpenRouter | Choose any model from openrouter.ai/models |
| `LANGSMITH_API_KEY` | No | [LangSmith](https://smith.langchain.com) | Full distributed tracing dashboard |
| `AMADEUS_CLIENT_ID` / `AMADEUS_CLIENT_SECRET` | No | [Amadeus Self-Service](https://developers.amadeus.com) | Live flight search and booking |
| `OPENWEATHER_API_KEY` | No | [OpenWeatherMap](https://openweathermap.org/api) | Real-time weather data |

Without the optional keys the system still runs completely — it returns clearly labelled sample/demo data instead of live data.

---

## 12. Testing

The test suite exercises the tool layer in isolation — no LLM calls, no network requests, no API keys required.

```bash
pip install pytest
python -m pytest tests/ -q
```

**What is tested:**

| Test | Covers |
|---|---|
| `test_search_returns_offers` | `search_flights` returns at least one offer in sample-data mode |
| `test_book_then_cancel` | Full booking + cancellation cycle; verifies 80% refund estimate |
| `test_seat_and_baggage` | Seat assignment + baggage add-on fees calculated correctly |
| `test_price_watch` | Price watch creation + check-status response shape |

These tests protect the tool layer from regressions independently of the LLM. Add integration tests (with a real LLM call) separately if you want to verify the full routing logic.

---

## 13. Current Limitations & Production Roadmap

### What is demo/simulated today

| Component | Current state | Production replacement |
|---|---|---|
| Flight data | Sample static offers when no Amadeus key is set | Amadeus Flight Offers Search API |
| Booking | In-memory store (`store.py`); data lost on restart | Postgres / DynamoDB with Amadeus Flight Create Orders |
| Seat map | Hardcoded seat types and prices | Airline ancillary catalog / SeatMap Display API |
| Baggage fees | Hardcoded fee table | Airline ancillary catalog |
| Price history | Deterministic hash-based fake data | Amadeus Flight Price Analysis endpoint |
| Price watch alerts | Simulated price comparison; no emails sent | Celery/cron job re-running `search_flights` + email dispatch |
| Weather | Real API if key provided; sample data otherwise | Already production-ready with key |
| Destination info | Hardcoded dictionary for 3 cities | Travel content API or knowledge base |

### Known architectural constraints

- **No parallelism:** The supervisor routes to one specialist at a time. A complex request (search + book + seat + weather) makes 4 sequential round-trips through the supervisor. A parallel fan-out architecture would be faster but more complex.
- **No memory across sessions:** Conversation history and bookings are in-memory only. Adding LangGraph checkpointing (e.g. `MemorySaver` or Postgres checkpointer) would enable multi-session continuity.
- **Recursion limit:** The graph is capped at 25 steps per invocation (`recursion_limit=25`). Very complex multi-step requests could hit this ceiling.
- **Single LLM instance:** The supervisor and all five specialists share the same model and temperature. Specialists that benefit from a lower temperature (booking confirmations) and those that benefit from a higher one (destination descriptions) are not independently tuned.

### Steps to go to production

1. Wire real Amadeus credentials and replace the `# TODO` stubs in `booking_tools.py`.
2. Replace `src/tools/store.py` with a database-backed implementation.
3. Add a scheduler (Celery Beat or AWS EventBridge) that periodically calls `check_price_watch` and sends alert emails.
4. Add LangGraph checkpointing for persistent multi-session conversations.
5. Deploy behind a REST or WebSocket API (FastAPI recommended) to serve a frontend.
6. Set `LANGSMITH_API_KEY` and `LANGSMITH_TRACING=true` in production to enable full observability.
