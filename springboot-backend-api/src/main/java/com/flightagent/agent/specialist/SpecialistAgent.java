package com.flightagent.agent.specialist;

import com.flightagent.agent.AgentState;

public interface SpecialistAgent {

    String name();

    String act(AgentState state);
}
