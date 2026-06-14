package com.flightagent.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RouteDecision(
        @JsonProperty(required = true) String next
) {
    public static final String FINISH = "FINISH";

    public boolean isFinish() {
        return next == null || FINISH.equalsIgnoreCase(next.trim());
    }
}
