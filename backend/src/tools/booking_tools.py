"""Booking tools: create, retrieve, cancel, and modify reservations.

Real API integration point: Amadeus "Flight Create Orders" / "Flight Order
Management". Those endpoints require pricing + traveler payloads, so the live
calls are stubbed with clearly marked TODOs; the in-memory store keeps the
demo fully functional offline.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Optional

from langchain_core.tools import tool

from . import store


@tool
def book_flight(
    offer_id: str,
    passenger_name: str,
    passenger_email: str,
    origin: str,
    destination: str,
    departure_date: str,
    price_total: str,
    currency: str = "USD",
) -> str:
    """Book a flight from a previously returned offer.

    Args:
        offer_id: The offer_id returned by search_flights.
        passenger_name: Full passenger name.
        passenger_email: Contact email for the booking confirmation.
        origin: Origin IATA code.
        destination: Destination IATA code.
        departure_date: Departure date YYYY-MM-DD.
        price_total: Total price as a string, e.g. '245.30'.
        currency: ISO currency code.

    Returns:
        JSON string with booking_id, status, and a PNR confirmation code.
    """
    # --- LIVE API (Amadeus Flight Create Orders) ---
    # client = _amadeus_client()
    # priced = client.shopping.flight_offers.pricing.post(offer)
    # order = client.booking.flight_orders.post(priced.data, travelers)
    # ----------------------------------------------------------------------
    record = {
        "offer_id": offer_id,
        "pnr": store.new_id("PNR").split("-", 1)[1],
        "status": "CONFIRMED",
        "passenger": {"name": passenger_name, "email": passenger_email},
        "itinerary": {
            "origin": origin,
            "destination": destination,
            "departure_date": departure_date,
        },
        "price": {"total": price_total, "currency": currency},
        "seat": None,
        "baggage": [],
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    bid = store.save_booking(record)
    return json.dumps(
        {"booking_id": bid, "pnr": record["pnr"], "status": "CONFIRMED",
         "summary": f"{origin}->{destination} on {departure_date} for "
                    f"{passenger_name}"},
        indent=2,
    )


@tool
def get_booking(booking_id: str) -> str:
    """Retrieve the full details of an existing booking.

    Args:
        booking_id: The booking_id returned by book_flight.

    Returns:
        JSON string with the booking record, or an error if not found.
    """
    rec = store.get_booking(booking_id)
    if rec is None:
        return json.dumps({"error": f"No booking found with id {booking_id}"})
    return json.dumps(rec, indent=2)


@tool
def cancel_booking(booking_id: str, reason: Optional[str] = None) -> str:
    """Cancel an existing booking.

    Args:
        booking_id: The booking_id to cancel.
        reason: Optional cancellation reason for the record.

    Returns:
        JSON string confirming cancellation and any refund estimate.
    """
    rec = store.get_booking(booking_id)
    if rec is None:
        return json.dumps({"error": f"No booking found with id {booking_id}"})
    if rec["status"] == "CANCELLED":
        return json.dumps({"booking_id": booking_id, "status": "CANCELLED",
                           "note": "Booking was already cancelled."})

    # --- LIVE API: client.booking.flight_orders(order_id).delete() ---
    rec["status"] = "CANCELLED"
    rec["cancel_reason"] = reason
    rec["cancelled_at"] = datetime.now(timezone.utc).isoformat()

    # Simple demo refund policy: 80% refundable.
    total = float(rec["price"]["total"])
    refund = round(total * 0.80, 2)
    return json.dumps(
        {"booking_id": booking_id, "status": "CANCELLED",
         "refund_estimate": {"amount": refund,
                             "currency": rec["price"]["currency"]}},
        indent=2,
    )


@tool
def modify_booking(
    booking_id: str,
    new_departure_date: Optional[str] = None,
    new_destination: Optional[str] = None,
) -> str:
    """Modify the dates or destination of an existing booking.

    Args:
        booking_id: The booking_id to modify.
        new_departure_date: New departure date YYYY-MM-DD (optional).
        new_destination: New destination IATA code (optional).

    Returns:
        JSON string with the updated itinerary and any change fee.
    """
    rec = store.get_booking(booking_id)
    if rec is None:
        return json.dumps({"error": f"No booking found with id {booking_id}"})
    if rec["status"] == "CANCELLED":
        return json.dumps({"error": "Cannot modify a cancelled booking."})

    changed = {}
    if new_departure_date:
        rec["itinerary"]["departure_date"] = new_departure_date
        changed["departure_date"] = new_departure_date
    if new_destination:
        rec["itinerary"]["destination"] = new_destination
        changed["destination"] = new_destination

    if not changed:
        return json.dumps({"error": "No changes provided."})

    rec["status"] = "MODIFIED"
    rec["modified_at"] = datetime.now(timezone.utc).isoformat()
    change_fee = {"amount": 75.00, "currency": rec["price"]["currency"]}
    return json.dumps(
        {"booking_id": booking_id, "status": "MODIFIED",
         "changes": changed, "change_fee": change_fee,
         "itinerary": rec["itinerary"]},
        indent=2,
    )


BOOKING_TOOLS = [book_flight, get_booking, cancel_booking, modify_booking]
