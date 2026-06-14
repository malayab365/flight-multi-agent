package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChatRequest(
        @NotBlank String message,
        List<ChatMessage> history
) {
    public List<ChatMessage> history() {
        return history == null ? List.of() : history;
    }
}
