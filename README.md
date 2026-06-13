# Flight Multi-Agent System (LangChain · LangGraph · LangSmith)

A supervisor-based multi-agent assistant for **flight search, booking, and the
full ancillary lifecycle**. Built with LangChain tools, orchestrated by a
LangGraph supervisor graph, and fully traced with LangSmith.

## Architecture

```
                        +-------------+
                user -> | supervisor  | <-------------------+
                        +------+------+                     |
                               | routes to one specialist   | specialist reports back
        +----------+-----------+-----------+-----------+
        v          v           v           v           v
   search_agent booking   ancillary    price_agent  info_agent
                _agent    _agent
        |          |           |           |           |
   search &    book /      seat map /   track price  weather /
   IATA codes  cancel /    select seat  check watch  destination
               modify      add baggage  price hist.  info
```

* **Supervisor** (`src/graph.py`) — an LLM router that reads the conversation
  and picks the next specialist via structured output, or `FINISH`.
* **Specialists** (`src/agents/specialists.py`) — each is a LangGraph
  `create_react_agent` with its own tool subset and system prompt.
* **Tools** (`src/tools/`) — LangChain `@tool` functions with **real API
  stubs** that fall back to clearly labelled sample data when no key is set.
* **LangSmith** (`src/config.py`) — tracing is auto-enabled on import.

## Capabilities

| Specialist        | Tools                                                       | Provider stub        |
|-------------------|------------------------------------------------------------|----------------------|
| `search_agent`    | `search_flights`, `get_airport_code`                       | Amadeus Self-Service |
| `booking_agent`   | `book_flight`, `get_booking`, `cancel_booking`, `modify_booking` | Amadeus Orders  |
| `ancillary_agent` | `get_seat_map`, `select_seat`, `add_baggage`               | airline catalog      |
| `price_agent`     | `track_price`, `check_price_watch`, `get_price_history`    | Amadeus Price Analysis |
| `info_agent`      | `get_destination_weather`, `get_destination_info`         | OpenWeatherMap       |

## Setup

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # then fill in your keys
```

Required keys: `ANTHROPIC_API_KEY` (LLM). Optional but recommended:
`LANGSMITH_API_KEY` (tracing), `AMADEUS_CLIENT_ID/SECRET` (live flights),
`OPENWEATHER_API_KEY` (weather). Without the provider keys the system still runs
end-to-end on sample data.

## Run

```bash
# one-shot
python -m src.main "Find flights from New York to London on 2026-08-01"

# interactive chat
python -m src.main
```

Example multi-step request the supervisor will fan out across agents:

> "Find a flight from JFK to LHR on 2026-08-01, book the cheapest for John Doe
> (john@x.com), add a window seat and one checked bag, then tell me the weather
> in London."

## Tests

```bash
pip install pytest
python -m pytest tests/ -q     # tool layer; no LLM or network needed
```

## Project layout

```
flight_multi_agent/
├── requirements.txt
├── .env.example
├── README.md
├── src/
│   ├── config.py          # env + LangSmith tracing setup
│   ├── llm.py             # ChatAnthropic factory
│   ├── state.py           # LangGraph shared state
│   ├── graph.py           # supervisor multi-agent graph
│   ├── main.py            # CLI entry point
│   ├── tools/             # search / booking / ancillary / price / weather
│   └── agents/            # specialist ReAct agents
└── tests/
    └── test_tools.py
```

## Going to production

* Swap `src/tools/store.py` (in-memory) for a real database.
* Fill the live-API `TODO` blocks in `booking_tools.py` with Amadeus Flight
  Create Orders / Order Management calls.
* Add a scheduler (cron/Celery) that runs `check_price_watch` and dispatches
  alert emails.
* Add `langgraph` checkpointing (e.g. `MemorySaver` / Postgres) for persistent,
  multi-session conversations.
```
