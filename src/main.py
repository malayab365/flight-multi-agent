"""CLI entry point for the flight multi-agent system.

Usage:
    python -m src.main                      # interactive chat
    python -m src.main "Find flights JFK to LHR on 2026-08-01"
"""

from __future__ import annotations

import sys

from langchain_core.messages import HumanMessage

from . import config  # noqa: F401  (import enables LangSmith tracing)
from .graph import get_app


def _print_response(state: dict) -> None:
    for msg in reversed(state["messages"]):
        content = getattr(msg, "content", "")
        if content and getattr(msg, "type", "") in ("ai", "human"):
            name = getattr(msg, "name", None)
            who = name or msg.type
            print(f"\n[{who}] {content}")
            break


def run_once(user_input: str) -> dict:
    app = get_app()
    return app.invoke(
        {"messages": [HumanMessage(content=user_input)], "next": None},
        config={"recursion_limit": 25},
    )


def interactive() -> None:
    app = get_app()
    history: list = []
    print("Flight assistant ready. Type 'exit' to quit.\n")
    while True:
        try:
            user_input = input("you> ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if user_input.lower() in {"exit", "quit"}:
            break
        if not user_input:
            continue
        history.append(HumanMessage(content=user_input))
        state = app.invoke({"messages": history, "next": None},
                           config={"recursion_limit": 25})
        history = state["messages"]
        _print_response(state)


def main() -> None:
    if len(sys.argv) > 1:
        query = " ".join(sys.argv[1:])
        _print_response(run_once(query))
    else:
        interactive()


if __name__ == "__main__":
    main()
