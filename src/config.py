"""Central configuration: loads env vars and wires up LangSmith tracing.

Importing this module is enough to enable LangSmith tracing for the whole
process (it sets the LANGSMITH_* / LANGCHAIN_* environment variables that the
LangChain + LangGraph runtimes read automatically).
"""

from __future__ import annotations

import os
from functools import lru_cache

from dotenv import load_dotenv

# Load .env once at import time.
load_dotenv()


class Settings:
    """Typed view over the environment configuration."""

    # --- LLM ---
    anthropic_api_key: str = os.getenv("ANTHROPIC_API_KEY", "")
    anthropic_model: str = os.getenv("ANTHROPIC_MODEL", "claude-sonnet-4-6")

    # --- LangSmith ---
    langsmith_tracing: bool = os.getenv("LANGSMITH_TRACING", "false").lower() == "true"
    langsmith_api_key: str = os.getenv("LANGSMITH_API_KEY", "")
    langsmith_project: str = os.getenv("LANGSMITH_PROJECT", "flight-multi-agent")
    langsmith_endpoint: str = os.getenv(
        "LANGSMITH_ENDPOINT", "https://api.smith.langchain.com"
    )

    # --- Amadeus ---
    amadeus_client_id: str = os.getenv("AMADEUS_CLIENT_ID", "")
    amadeus_client_secret: str = os.getenv("AMADEUS_CLIENT_SECRET", "")
    amadeus_hostname: str = os.getenv("AMADEUS_HOSTNAME", "test")

    # --- SerpAPI (alternative) ---
    serpapi_api_key: str = os.getenv("SERPAPI_API_KEY", "")

    # --- Weather ---
    openweather_api_key: str = os.getenv("OPENWEATHER_API_KEY", "")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


def configure_langsmith() -> None:
    """Export the env vars LangChain/LangGraph read for tracing.

    LangChain inspects LANGCHAIN_TRACING_V2 / LANGSMITH_* at runtime. We mirror
    the LANGSMITH_* values into both naming schemes so tracing works regardless
    of the installed library version.
    """
    s = get_settings()
    if not s.langsmith_tracing:
        return

    os.environ.setdefault("LANGCHAIN_TRACING_V2", "true")
    os.environ.setdefault("LANGSMITH_TRACING", "true")
    os.environ.setdefault("LANGCHAIN_ENDPOINT", s.langsmith_endpoint)
    os.environ.setdefault("LANGSMITH_ENDPOINT", s.langsmith_endpoint)
    os.environ.setdefault("LANGCHAIN_PROJECT", s.langsmith_project)
    os.environ.setdefault("LANGSMITH_PROJECT", s.langsmith_project)
    if s.langsmith_api_key:
        os.environ.setdefault("LANGCHAIN_API_KEY", s.langsmith_api_key)
        os.environ.setdefault("LANGSMITH_API_KEY", s.langsmith_api_key)


# Configure on import so every entry point is traced.
configure_langsmith()
