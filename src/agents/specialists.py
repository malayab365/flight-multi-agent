"""Specialist ReAct agents, one per domain.

Each specialist is a small LangGraph ReAct agent (LLM + its own tool subset)
built with `create_react_agent`. The supervisor (see ../graph.py) decides which
specialist handles each turn.
"""

from __future__ import annotations

from langgraph.prebuilt import create_react_agent

from ..llm import get_llm
from ..tools import (
    SEARCH_TOOLS,
    BOOKING_TOOLS,
    ANCILLARY_TOOLS,
    PRICE_TOOLS,
    WEATHER_TOOLS,
)

# Name -> (system prompt, tools). The keys are the routing labels used by the
# supervisor, so keep them in sync with graph.MEMBERS.
SPECIALIST_SPECS = {
    "search_agent": (
        "You are a flight search specialist. Use the search tools to find "
        "flights and resolve city names to IATA codes. Always present offers "
        "with their offer_id, times, stops, and price so the user (or another "
        "agent) can book them.",
        SEARCH_TOOLS,
    ),
    "booking_agent": (
        "You are a booking specialist. You create, look up, cancel, and modify "
        "flight bookings. Confirm key details (passenger, route, date, price) "
        "before booking, and always report the booking_id and PNR.",
        BOOKING_TOOLS,
    ),
    "ancillary_agent": (
        "You handle seat selection and baggage add-ons for existing bookings. "
        "Show the seat map / fees when helpful and confirm the booking_id "
        "before making changes.",
        ANCILLARY_TOOLS,
    ),
    "price_agent": (
        "You are a fare-tracking specialist. You create price watches, check "
        "them, and provide historical price context to advise on whether a "
        "fare is a good deal.",
        PRICE_TOOLS,
    ),
    "info_agent": (
        "You provide destination weather and practical travel information to "
        "help the traveler plan their trip.",
        WEATHER_TOOLS,
    ),
}


def build_specialists():
    """Instantiate every specialist agent. Returns dict[name -> runnable]."""
    llm = get_llm()
    return {
        name: create_react_agent(llm, tools, prompt=prompt)
        for name, (prompt, tools) in SPECIALIST_SPECS.items()
    }
