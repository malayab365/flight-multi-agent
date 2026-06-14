"""Flight search tools.

Real API integration: Amadeus Self-Service "Flight Offers Search".
If Amadeus credentials are not configured, the tool falls back to a clearly
labelled sample response so the graph stays runnable end-to-end offline.
"""

from __future__ import annotations

import json
from typing import Optional

from langchain_core.tools import tool

from ..config import get_settings


def _amadeus_client():
    """Build an Amadeus client, or return None if not configured/installed."""
    s = get_settings()
    if not (s.amadeus_client_id and s.amadeus_client_secret):
        return None
    try:
        from amadeus import Client  # imported lazily so the dep is optional
    except ImportError:
        return None
    return Client(
        client_id=s.amadeus_client_id,
        client_secret=s.amadeus_client_secret,
        hostname=s.amadeus_hostname,
    )


def _sample_offers(origin: str, destination: str, departure_date: str) -> list[dict]:
    """Deterministic placeholder used when no live API key is present."""
    return [
        {
            "offer_id": "SAMPLE-1",
            "airline": "AA",
            "flight_number": "AA100",
            "origin": origin,
            "destination": destination,
            "departure_date": departure_date,
            "depart_time": "08:30",
            "arrive_time": "11:45",
            "stops": 0,
            "cabin": "ECONOMY",
            "price": {"total": "245.30", "currency": "USD"},
            "seats_remaining": 7,
        },
        {
            "offer_id": "SAMPLE-2",
            "airline": "DL",
            "flight_number": "DL205",
            "origin": origin,
            "destination": destination,
            "departure_date": departure_date,
            "depart_time": "13:10",
            "arrive_time": "17:55",
            "stops": 1,
            "cabin": "ECONOMY",
            "price": {"total": "198.00", "currency": "USD"},
            "seats_remaining": 3,
        },
    ]


@tool
def search_flights(
    origin: str,
    destination: str,
    departure_date: str,
    return_date: Optional[str] = None,
    adults: int = 1,
    travel_class: str = "ECONOMY",
    max_results: int = 5,
) -> str:
    """Search for available flights between two airports.

    Args:
        origin: Origin IATA airport code, e.g. 'JFK'.
        destination: Destination IATA airport code, e.g. 'LHR'.
        departure_date: Departure date in YYYY-MM-DD.
        return_date: Optional return date in YYYY-MM-DD for round trips.
        adults: Number of adult passengers.
        travel_class: ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST.
        max_results: Maximum number of offers to return.

    Returns:
        JSON string with a list of flight offers (offer_id, airline, times,
        stops, price, seats_remaining).
    """
    client = _amadeus_client()
    if client is None:
        offers = _sample_offers(origin, destination, departure_date)[:max_results]
        return json.dumps(
            {"source": "sample_data", "offers": offers}, indent=2
        )

    try:
        params = dict(
            originLocationCode=origin,
            destinationLocationCode=destination,
            departureDate=departure_date,
            adults=adults,
            travelClass=travel_class,
            currencyCode="USD",
            max=max_results,
        )
        if return_date:
            params["returnDate"] = return_date

        resp = client.shopping.flight_offers_search.get(**params)
        offers = []
        for o in resp.data:
            itin = o["itineraries"][0]["segments"]
            first, last = itin[0], itin[-1]
            offers.append(
                {
                    "offer_id": o["id"],
                    "airline": first["carrierCode"],
                    "flight_number": first["carrierCode"] + first["number"],
                    "origin": first["departure"]["iataCode"],
                    "destination": last["arrival"]["iataCode"],
                    "departure_date": departure_date,
                    "depart_time": first["departure"]["at"],
                    "arrive_time": last["arrival"]["at"],
                    "stops": len(itin) - 1,
                    "cabin": travel_class,
                    "price": {
                        "total": o["price"]["total"],
                        "currency": o["price"]["currency"],
                    },
                    "seats_remaining": o.get("numberOfBookableSeats"),
                }
            )
        return json.dumps({"source": "amadeus", "offers": offers}, indent=2)
    except Exception as exc:  # noqa: BLE001 - surface API errors to the agent
        return json.dumps({"error": f"Amadeus search failed: {exc}"})


@tool
def get_airport_code(city_name: str) -> str:
    """Resolve a city or airport name to its IATA code using Amadeus locations.

    Args:
        city_name: A city or airport name, e.g. 'London' or 'Heathrow'.

    Returns:
        JSON string with matching locations (name, IATA code, country).
    """
    client = _amadeus_client()
    if client is None:
        common = {
            "new york": "NYC", "london": "LON", "paris": "PAR",
            "tokyo": "TYO", "delhi": "DEL", "dubai": "DXB",
            "san francisco": "SFO", "los angeles": "LAX",
        }
        code = common.get(city_name.strip().lower())
        return json.dumps(
            {"source": "sample_data",
             "matches": [{"name": city_name, "iata": code}] if code else []}
        )
    try:
        resp = client.reference_data.locations.get(
            keyword=city_name, subType="CITY,AIRPORT"
        )
        matches = [
            {"name": d["name"], "iata": d["iataCode"],
             "country": d["address"].get("countryCode")}
            for d in resp.data
        ]
        return json.dumps({"source": "amadeus", "matches": matches})
    except Exception as exc:  # noqa: BLE001
        return json.dumps({"error": f"Amadeus location lookup failed: {exc}"})


SEARCH_TOOLS = [search_flights, get_airport_code]
