"""Smoke tests for the tool layer (no LLM / network required).

Run with:  python -m pytest tests/ -q
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.tools import search_tools, booking_tools, ancillary_tools, price_tools


def test_search_returns_offers():
    out = json.loads(search_tools.search_flights.invoke(
        {"origin": "JFK", "destination": "LHR", "departure_date": "2026-08-01"}))
    assert "offers" in out and len(out["offers"]) >= 1


def test_book_then_cancel():
    booked = json.loads(booking_tools.book_flight.invoke({
        "offer_id": "SAMPLE-1", "passenger_name": "Ada Lovelace",
        "passenger_email": "ada@example.com", "origin": "JFK",
        "destination": "LHR", "departure_date": "2026-08-01",
        "price_total": "245.30"}))
    bid = booked["booking_id"]
    assert booked["status"] == "CONFIRMED"

    cancelled = json.loads(booking_tools.cancel_booking.invoke(
        {"booking_id": bid, "reason": "test"}))
    assert cancelled["status"] == "CANCELLED"
    assert cancelled["refund_estimate"]["amount"] > 0


def test_seat_and_baggage():
    booked = json.loads(booking_tools.book_flight.invoke({
        "offer_id": "SAMPLE-2", "passenger_name": "Alan Turing",
        "passenger_email": "alan@example.com", "origin": "SFO",
        "destination": "NRT", "departure_date": "2026-09-10",
        "price_total": "780.00"}))
    bid = booked["booking_id"]

    seat = json.loads(ancillary_tools.select_seat.invoke(
        {"booking_id": bid, "seat_type": "WINDOW", "seat_number": "12A"}))
    assert seat["status"] == "SEAT_ASSIGNED"

    bag = json.loads(ancillary_tools.add_baggage.invoke(
        {"booking_id": bid, "baggage_type": "CHECKED_23KG", "quantity": 2}))
    assert bag["added"]["fee"] == 70.0


def test_price_watch():
    watch = json.loads(price_tools.track_price.invoke({
        "origin": "JFK", "destination": "LAX", "departure_date": "2026-07-01",
        "target_price": 150.0, "email": "u@example.com"}))
    assert watch["status"] == "ACTIVE"
    chk = json.loads(price_tools.check_price_watch.invoke(
        {"watch_id": watch["watch_id"]}))
    assert "current_price" in chk
