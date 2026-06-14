package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BaggageRequest(
        @NotBlank String baggageType,
        Integer quantity
) {
    public int quantityOrDefault() {
        return quantity == null ? 1 : quantity;
    }
}
