package com.flightagent.agent;

public record AgentMessage(Role role, String content, String name) {

    public enum Role { USER, ASSISTANT, SYSTEM }

    public static AgentMessage user(String content) {
        return new AgentMessage(Role.USER, content, null);
    }

    public static AgentMessage assistant(String content, String name) {
        return new AgentMessage(Role.ASSISTANT, content, name);
    }

    public static AgentMessage system(String content) {
        return new AgentMessage(Role.SYSTEM, content, null);
    }
}
