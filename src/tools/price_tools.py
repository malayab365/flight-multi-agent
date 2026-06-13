"""Price tracking and fare-alert tools.

Real API integration point: Amadeus "Flight Price Analysis" for historical
fare context, plus a scheduler (cron / Celery) that re-runs search_flights and
fires alerts. Here the watch is persisted in-memory and a check is simulated.
"""

from __future__ import annotations

import json
import random
from datetime import datetime, timezone

from langchain_core.tools import tool

from . import store


@tool
def track_price(
    origin: str,
    destination: str,
    departure_date: str,
    target_price: float,
    email: str,
) -> str:
    """Create a price watch that alerts when a fare drops to/below a target.

    Args:
        origin: Origin IATA code.
        destination: Destination IATA code.
        departure_date: Departure date YYYY-MM-DD.
        target_price: Alert when the total fare is at or below this value.
        email: Where to send the alert.

    Returns:
        JSON string with the watch_id and status.
    """
    record = {
        "route": f"{origin}-{destination}",
        "departure_date": departure_date,
        "target_price": target_price,
        "email": email,
        "status": "ACTIVE",
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    wid = store.save_watch(record)
    return json.dumps(
        {"watch_id": wid, "status": "ACTIVE",
         "message": f"Tracking {origin}->{destination} on {departure_date}; "
                    f"will alert {email} at <= {target_price}."},
        indent=2,
    )


@tool
def check_price_watch(watch_id: str) -> str:
    """Check the current fare for an active price watch.

    Args:
        watch_id: The watch_id returned by track_price.

    Returns:
        JSON string with the current price and whether the alert triggered.
        (Live build: re-run search_flights here instead of simulating.)
    """
    rec = store.PRICE_WATCHES.get(watch_id)
    if rec is None:
        return json.dumps({"error": f"No price watch with id {watch_id}"})

    # Simulated current fare around the target for demo purposes.
    target = rec["target_price"]
    current = round(target * random.uniform(0.85, 1.25), 2)
    triggered = current <= target
    return json.dumps(
        {"watch_id": watch_id, "route": rec["route"],
         "current_price": current, "target_price": target,
         "alert_triggered": triggered,
         "action": "Notify user" if triggered else "Keep watching"},
        indent=2,
    )


@tool
def get_price_history(origin: str, destination: str, departure_date: str) -> str:
    """Return historical fare context for a route (cheap/typical/expensive).

    Args:
        origin: Origin IATA code.
        destination: Destination IATA code.
        departure_date: Departure date YYYY-MM-DD.

    Returns:
        JSON string with price quartiles. Live build: Amadeus Flight Price
        Analysis endpoint.
    """
    base = 200 + (hash(origin + destination) % 300)
    return json.dumps(
        {"route": f"{origin}-{destination}", "departure_date": departure_date,
         "currency": "USD",
         "quartiles": {"minimum": base * 0.7, "first": base * 0.85,
                       "median": base, "third": base * 1.2,
                       "maximum": base * 1.6},
         "note": "Sample distribution; wire to Amadeus Flight Price Analysis."},
        indent=2,
    )


PRICE_TOOLS = [track_price, check_price_watch, get_price_history]
