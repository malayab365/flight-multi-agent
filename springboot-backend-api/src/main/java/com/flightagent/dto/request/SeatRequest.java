package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SeatRequest(
        @NotBlank String seatType,
        String seatNumber
) {
}
