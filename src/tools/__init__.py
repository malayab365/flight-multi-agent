"""Tool collections grouped by domain."""

from .search_tools import SEARCH_TOOLS
from .booking_tools import BOOKING_TOOLS
from .ancillary_tools import ANCILLARY_TOOLS
from .price_tools import PRICE_TOOLS
from .weather_tools import WEATHER_TOOLS

ALL_TOOLS = (
    SEARCH_TOOLS + BOOKING_TOOLS + ANCILLARY_TOOLS + PRICE_TOOLS + WEATHER_TOOLS
)

__all__ = [
    "SEARCH_TOOLS",
    "BOOKING_TOOLS",
    "ANCILLARY_TOOLS",
    "PRICE_TOOLS",
    "WEATHER_TOOLS",
    "ALL_TOOLS",
]
