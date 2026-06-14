package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PriceTrackRequest(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotBlank String departureDate,
        @NotNull Double targetPrice,
        @NotBlank String email
) {
}
