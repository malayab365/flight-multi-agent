"""Tiny in-memory persistence layer shared across booking-related tools.

In a production system replace this with a real database (Postgres, DynamoDB,
etc.). It is intentionally process-local so the demo runs with zero infra.
"""

from __future__ import annotations

import threading
import uuid
from typing import Any, Dict

_lock = threading.Lock()

# booking_id -> booking record
BOOKINGS: Dict[str, Dict[str, Any]] = {}

# price-watch_id -> watch record
PRICE_WATCHES: Dict[str, Dict[str, Any]] = {}


def new_id(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:8].upper()}"


def save_booking(record: Dict[str, Any]) -> str:
    with _lock:
        bid = record.get("booking_id") or new_id("BK")
        record["booking_id"] = bid
        BOOKINGS[bid] = record
        return bid


def get_booking(booking_id: str) -> Dict[str, Any] | None:
    return BOOKINGS.get(booking_id)


def save_watch(record: Dict[str, Any]) -> str:
    with _lock:
        wid = record.get("watch_id") or new_id("PW")
        record["watch_id"] = wid
        PRICE_WATCHES[wid] = record
        return wid
