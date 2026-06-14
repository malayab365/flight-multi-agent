package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank String offerId,
        @NotBlank String passengerName,
        @NotBlank String passengerEmail,
        @NotBlank String origin,
        @NotBlank String destination,
        @NotBlank String departureDate,
        @NotBlank String priceTotal,
        String currency
) {
    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "USD" : currency;
    }
}
