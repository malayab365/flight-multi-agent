"""LangGraph supervisor multi-agent graph.

Architecture (supervisor / router pattern):

                +-------------+
        user -> | supervisor  | <-----------------+
                +------+------+                    |
                       | routes to one member      | each member reports back
        +--------------+-----------------+         |
        v       v        v        v       v        |
   search   booking  ancillary  price   info  ----+
   _agent   _agent   _agent    _agent  _agent

The supervisor inspects the running conversation and picks the next specialist
(or FINISH). Each specialist is a ReAct agent with its own tools. Everything is
traced to LangSmith automatically via config.py.
"""

from __future__ import annotations

from typing import Literal

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, Field

from .agents import build_specialists
from .llm import get_llm
from .state import AgentState

MEMBERS = [
    "search_agent",
    "booking_agent",
    "ancillary_agent",
    "price_agent",
    "info_agent",
]
_ROUTE_OPTIONS = MEMBERS + ["FINISH"]

SUPERVISOR_SYSTEM = (
    "You are the supervisor of a flight-assistant team. Given the conversation "
    "so far, decide which team member should act next, or FINISH when the "
    "user's request is fully handled.\n\n"
    "Members:\n"
    "- search_agent: find flights, resolve airport/city codes.\n"
    "- booking_agent: create, retrieve, cancel, or modify bookings.\n"
    "- ancillary_agent: seat selection and baggage add-ons.\n"
    "- price_agent: price tracking, fare alerts, price history.\n"
    "- info_agent: destination weather and travel info.\n\n"
    "Route to exactly one member at a time. When the last specialist has fully "
    "answered the user, respond with FINISH."
)


class Route(BaseModel):
    """Structured routing decision returned by the supervisor."""

    next: Literal[
        "search_agent", "booking_agent", "ancillary_agent",
        "price_agent", "info_agent", "FINISH",
    ] = Field(description="The next member to act, or FINISH.")


def _supervisor_node(state: AgentState) -> dict:
    llm = get_llm()
    router = llm.with_structured_output(Route)
    messages = [SystemMessage(content=SUPERVISOR_SYSTEM), *state["messages"]]
    decision = router.invoke(messages)
    return {"next": decision.next}


def _make_member_node(name: str, agent):
    """Wrap a specialist ReAct agent as a graph node.

    The specialist's final message is re-emitted as a named message so the
    supervisor can see who said what on the next routing decision.
    """

    def node(state: AgentState) -> dict:
        result = agent.invoke({"messages": state["messages"]})
        last = result["messages"][-1]
        return {
            "messages": [HumanMessage(content=last.content, name=name)],
        }

    return node


def _route_from_supervisor(state: AgentState) -> str:
    nxt = state.get("next")
    return END if nxt in (None, "FINISH") else nxt


def build_graph():
    """Compile and return the multi-agent graph."""
    specialists = build_specialists()

    graph = StateGraph(AgentState)
    graph.add_node("supervisor", _supervisor_node)
    for name in MEMBERS:
        graph.add_node(name, _make_member_node(name, specialists[name]))

    graph.add_edge(START, "supervisor")
    # Supervisor conditionally routes to a member or ends.
    graph.add_conditional_edges(
        "supervisor",
        _route_from_supervisor,
        {**{m: m for m in MEMBERS}, END: END},
    )
    # Every member reports back to the supervisor.
    for name in MEMBERS:
        graph.add_edge(name, "supervisor")

    return graph.compile()


# Convenience singleton.
app = None


def get_app():
    global app
    if app is None:
        app = build_graph()
    return app
