package com.flightagent.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentState {

    private final List<AgentMessage> messages = new ArrayList<>();

    public AgentState() {}

    public AgentState(List<AgentMessage> seed) {
        if (seed != null) {
            messages.addAll(seed);
        }
    }

    public void append(AgentMessage message) {
        messages.add(message);
    }

    public void appendAll(List<AgentMessage> batch) {
        if (batch != null) {
            messages.addAll(batch);
        }
    }

    public List<AgentMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    public AgentMessage lastAssistant() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == AgentMessage.Role.ASSISTANT) {
                return messages.get(i);
            }
        }
        return null;
    }

    public int size() {
        return messages.size();
    }
}
