"""Destination weather & info tools (OpenWeatherMap real API stub)."""

from __future__ import annotations

import json

import requests

from langchain_core.tools import tool

from ..config import get_settings


@tool
def get_destination_weather(city_name: str) -> str:
    """Get the current weather for a destination city.

    Args:
        city_name: Destination city name, e.g. 'Tokyo'.

    Returns:
        JSON string with temperature, conditions, and humidity. Falls back to
        sample data when OPENWEATHER_API_KEY is not set.
    """
    s = get_settings()
    if not s.openweather_api_key:
        return json.dumps(
            {"source": "sample_data", "city": city_name,
             "temp_c": 21.0, "conditions": "Partly cloudy", "humidity": 60},
        )
    try:
        resp = requests.get(
            "https://api.openweathermap.org/data/2.5/weather",
            params={"q": city_name, "appid": s.openweather_api_key,
                    "units": "metric"},
            timeout=15,
        )
        resp.raise_for_status()
        d = resp.json()
        return json.dumps(
            {"source": "openweathermap", "city": city_name,
             "temp_c": d["main"]["temp"],
             "conditions": d["weather"][0]["description"],
             "humidity": d["main"]["humidity"]},
            indent=2,
        )
    except Exception as exc:  # noqa: BLE001
        return json.dumps({"error": f"Weather lookup failed: {exc}"})


@tool
def get_destination_info(city_name: str) -> str:
    """Get practical travel info for a destination (currency, tz, tips).

    Args:
        city_name: Destination city name.

    Returns:
        JSON string with destination details. Replace the lookup with a real
        travel-content API or knowledge base in production.
    """
    info = {
        "tokyo": {"country": "Japan", "currency": "JPY", "timezone": "JST (UTC+9)",
                  "language": "Japanese", "tip": "Carry cash; many places are card-shy."},
        "london": {"country": "UK", "currency": "GBP", "timezone": "GMT/BST",
                   "language": "English", "tip": "Get an Oyster card for the Tube."},
        "paris": {"country": "France", "currency": "EUR", "timezone": "CET",
                  "language": "French", "tip": "Many museums close on Mondays."},
    }
    key = city_name.strip().lower()
    return json.dumps(
        {"city": city_name,
         "info": info.get(key, {"note": "No cached info; wire to a travel API."})},
        indent=2,
    )


WEATHER_TOOLS = [get_destination_weather, get_destination_info]
