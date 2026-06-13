"""Anthropic chat model factory."""

from __future__ import annotations

from functools import lru_cache

from langchain_anthropic import ChatAnthropic

from .config import get_settings


@lru_cache(maxsize=4)
def get_llm(temperature: float = 0.0) -> ChatAnthropic:
    """Return a cached ChatAnthropic instance.

    The LangSmith env vars are already set by config.py, so every call made
    through this model is traced automatically.
    """
    s = get_settings()
    return ChatAnthropic(
        model=s.anthropic_model,
        temperature=temperature,
        api_key=s.anthropic_api_key,
        max_tokens=2048,
        timeout=60,
    )
