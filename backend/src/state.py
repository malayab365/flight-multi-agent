"""Shared graph state for the multi-agent system."""

from __future__ import annotations

from typing import Annotated, Optional, TypedDict

from langgraph.graph.message import add_messages


class AgentState(TypedDict):
    """State passed between the supervisor and specialist agents.

    `messages` accumulates the full conversation (the `add_messages` reducer
    appends rather than overwrites). `next` is set by the supervisor to route
    to the chosen specialist.
    """

    messages: Annotated[list, add_messages]
    next: Optional[str]
