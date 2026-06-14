"""OpenRouter chat model factory."""

from __future__ import annotations

from functools import lru_cache

from langchain_openai import ChatOpenAI

from .config import get_settings

_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"


@lru_cache(maxsize=4)
def get_llm(temperature: float = 0.0) -> ChatOpenAI:
    """Return a cached ChatOpenAI instance pointed at OpenRouter.

    The LangSmith env vars are already set by config.py, so every call made
    through this model is traced automatically.
    """
    s = get_settings()
    return ChatOpenAI(
        model=s.openrouter_model,
        temperature=temperature,
        api_key=s.openrouter_api_key,
        base_url=_OPENROUTER_BASE_URL,
        max_tokens=2048,
        timeout=60,
    )
