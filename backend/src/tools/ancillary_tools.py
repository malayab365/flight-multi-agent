"""Ancillary services: seat selection and baggage add-ons."""

from __future__ import annotations

import json
from typing import Optional

from langchain_core.tools import tool

from . import store

# Demo seat map / pricing. Replace with airline ancillary catalog API.
_SEAT_FEES = {"WINDOW": 18.0, "AISLE": 15.0, "MIDDLE": 0.0,
              "EXIT_ROW": 39.0, "BULKHEAD": 29.0}
_BAGGAGE_FEES = {"CHECKED_23KG": 35.0, "CHECKED_32KG": 60.0,
                 "EXTRA_CARRYON": 25.0, "SPORTS_EQUIPMENT": 70.0}


@tool
def get_seat_map(offer_id: str) -> str:
    """Return the available seat types and fees for an offer.

    Args:
        offer_id: The flight offer_id.

    Returns:
        JSON string mapping seat type -> fee (USD). Replace with the airline
        SeatMap Display API in production.
    """
    return json.dumps({"offer_id": offer_id, "currency": "USD",
                       "seat_types": _SEAT_FEES}, indent=2)


@tool
def select_seat(booking_id: str, seat_type: str, seat_number: Optional[str] = None) -> str:
    """Assign a seat to an existing booking.

    Args:
        booking_id: The booking_id to update.
        seat_type: One of WINDOW, AISLE, MIDDLE, EXIT_ROW, BULKHEAD.
        seat_number: Optional specific seat, e.g. '14A'.

    Returns:
        JSON string confirming the seat assignment and fee.
    """
    rec = store.get_booking(booking_id)
    if rec is None:
        return json.dumps({"error": f"No booking found with id {booking_id}"})
    seat_type = seat_type.upper()
    if seat_type not in _SEAT_FEES:
        return json.dumps({"error": f"Unknown seat_type. Choose from "
                                    f"{list(_SEAT_FEES)}"})
    fee = _SEAT_FEES[seat_type]
    rec["seat"] = {"type": seat_type, "number": seat_number, "fee": fee}
    return json.dumps(
        {"booking_id": booking_id, "seat": rec["seat"],
         "status": "SEAT_ASSIGNED"}, indent=2,
    )


@tool
def add_baggage(booking_id: str, baggage_type: str, quantity: int = 1) -> str:
    """Add checked or special baggage to a booking.

    Args:
        booking_id: The booking_id to update.
        baggage_type: One of CHECKED_23KG, CHECKED_32KG, EXTRA_CARRYON,
            SPORTS_EQUIPMENT.
        quantity: Number of units to add.

    Returns:
        JSON string confirming the baggage and total fee.
    """
    rec = store.get_booking(booking_id)
    if rec is None:
        return json.dumps({"error": f"No booking found with id {booking_id}"})
    baggage_type = baggage_type.upper()
    if baggage_type not in _BAGGAGE_FEES:
        return json.dumps({"error": f"Unknown baggage_type. Choose from "
                                    f"{list(_BAGGAGE_FEES)}"})
    fee = round(_BAGGAGE_FEES[baggage_type] * quantity, 2)
    item = {"type": baggage_type, "quantity": quantity, "fee": fee}
    rec.setdefault("baggage", []).append(item)
    return json.dumps(
        {"booking_id": booking_id, "added": item,
         "all_baggage": rec["baggage"], "status": "BAGGAGE_ADDED"}, indent=2,
    )


ANCILLARY_TOOLS = [get_seat_map, select_seat, add_baggage]
