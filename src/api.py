"""FastAPI REST gateway — exposes every tool and the multi-agent chat as HTTP endpoints."""

from __future__ import annotations

import json
from typing import Any, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from langchain_core.messages import HumanMessage
from pydantic import BaseModel

from . import config  # noqa: F401  — enables LangSmith tracing on import
from .graph import get_app
from .tools.ancillary_tools import add_baggage, get_seat_map, select_seat
from .tools.booking_tools import (
    book_flight,
    cancel_booking,
    get_booking,
    modify_booking,
)
from .tools.price_tools import check_price_watch, get_price_history, track_price
from .tools.search_tools import get_airport_code, search_flights
from .tools.weather_tools import get_destination_info, get_destination_weather

app = FastAPI(title="Flight Multi-Agent API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _json(result: str) -> Any:
    try:
        return json.loads(result)
    except json.JSONDecodeError:
        return {"raw": result}


# ── Request models ────────────────────────────────────────────────────────────

class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str
    history: list[ChatMessage] = []


class SearchRequest(BaseModel):
    origin: str
    destination: str
    departure_date: str
    return_date: Optional[str] = None
    adults: int = 1
    travel_class: str = "ECONOMY"
    max_results: int = 8


class BookRequest(BaseModel):
    offer_id: str
    passenger_name: str
    passenger_email: str
    origin: str
    destination: str
    departure_date: str
    price_total: str
    currency: str = "USD"


class CancelRequest(BaseModel):
    reason: Optional[str] = None


class ModifyRequest(BaseModel):
    new_departure_date: Optional[str] = None
    new_destination: Optional[str] = None


class SeatRequest(BaseModel):
    seat_type: str
    seat_number: Optional[str] = None


class BaggageRequest(BaseModel):
    baggage_type: str
    quantity: int = 1


class PriceTrackRequest(BaseModel):
    origin: str
    destination: str
    departure_date: str
    target_price: float
    email: str


# ── Chat ─────────────────────────────────────────────────────────────────────

@app.post("/api/chat")
def chat(req: ChatRequest):
    agent = get_app()
    messages = [HumanMessage(content=m.content) for m in req.history]
    messages.append(HumanMessage(content=req.message))
    state = agent.invoke(
        {"messages": messages, "next": None},
        config={"recursion_limit": 25},
    )
    for msg in reversed(state["messages"]):
        content = getattr(msg, "content", "")
        if content and getattr(msg, "type", "") in ("ai", "human"):
            name = getattr(msg, "name", None)
            return {"response": content, "agent": name or msg.type}
    return {"response": "No response generated.", "agent": "system"}


# ── Search ────────────────────────────────────────────────────────────────────

@app.post("/api/search")
def search(req: SearchRequest):
    result = search_flights.invoke({
        "origin": req.origin,
        "destination": req.destination,
        "departure_date": req.departure_date,
        "return_date": req.return_date,
        "adults": req.adults,
        "travel_class": req.travel_class,
        "max_results": req.max_results,
    })
    return _json(result)


@app.get("/api/airport-code")
def airport_code(city: str):
    return _json(get_airport_code.invoke({"city_name": city}))


# ── Booking ───────────────────────────────────────────────────────────────────

@app.post("/api/book")
def book(req: BookRequest):
    result = book_flight.invoke({
        "offer_id": req.offer_id,
        "passenger_name": req.passenger_name,
        "passenger_email": req.passenger_email,
        "origin": req.origin,
        "destination": req.destination,
        "departure_date": req.departure_date,
        "price_total": req.price_total,
        "currency": req.currency,
    })
    return _json(result)


@app.get("/api/booking/{booking_id}")
def booking_detail(booking_id: str):
    result = _json(get_booking.invoke({"booking_id": booking_id}))
    if "error" in result:
        raise HTTPException(status_code=404, detail=result["error"])
    return result


@app.post("/api/booking/{booking_id}/cancel")
def cancel(booking_id: str, req: CancelRequest):
    return _json(cancel_booking.invoke({"booking_id": booking_id, "reason": req.reason}))


@app.post("/api/booking/{booking_id}/modify")
def modify(booking_id: str, req: ModifyRequest):
    return _json(modify_booking.invoke({
        "booking_id": booking_id,
        "new_departure_date": req.new_departure_date,
        "new_destination": req.new_destination,
    }))


# ── Ancillary ─────────────────────────────────────────────────────────────────

@app.get("/api/seat-map/{offer_id}")
def seat_map(offer_id: str):
    return _json(get_seat_map.invoke({"offer_id": offer_id}))


@app.post("/api/booking/{booking_id}/seat")
def assign_seat(booking_id: str, req: SeatRequest):
    return _json(select_seat.invoke({
        "booking_id": booking_id,
        "seat_type": req.seat_type,
        "seat_number": req.seat_number,
    }))


@app.post("/api/booking/{booking_id}/baggage")
def add_bag(booking_id: str, req: BaggageRequest):
    return _json(add_baggage.invoke({
        "booking_id": booking_id,
        "baggage_type": req.baggage_type,
        "quantity": req.quantity,
    }))


# ── Price ─────────────────────────────────────────────────────────────────────

@app.post("/api/price/track")
def price_track(req: PriceTrackRequest):
    return _json(track_price.invoke({
        "origin": req.origin,
        "destination": req.destination,
        "departure_date": req.departure_date,
        "target_price": req.target_price,
        "email": req.email,
    }))


@app.get("/api/price/watch/{watch_id}")
def price_watch(watch_id: str):
    return _json(check_price_watch.invoke({"watch_id": watch_id}))


@app.get("/api/price/history")
def price_history(origin: str, destination: str, departure_date: str):
    return _json(get_price_history.invoke({
        "origin": origin,
        "destination": destination,
        "departure_date": departure_date,
    }))


# ── Info ──────────────────────────────────────────────────────────────────────

@app.get("/api/weather/{city}")
def weather(city: str):
    return _json(get_destination_weather.invoke({"city_name": city}))


@app.get("/api/destination/{city}")
def destination_info(city: str):
    return _json(get_destination_info.invoke({"city_name": city}))
