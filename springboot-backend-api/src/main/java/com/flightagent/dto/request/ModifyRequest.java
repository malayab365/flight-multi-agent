package com.flightagent.dto.request;

public record ModifyRequest(
        String newDepartureDate,
        String newDestination
) {
}
