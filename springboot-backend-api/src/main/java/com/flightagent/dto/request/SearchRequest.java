package com.flightagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotBlank String departureDate,
        String returnDate,
        Integer adults,
        String travelClass,
        Integer maxResults
) {
    public int adultsOrDefault() {
        return adults == null ? 1 : adults;
    }

    public String travelClassOrDefault() {
        return travelClass == null || travelClass.isBlank() ? "ECONOMY" : travelClass;
    }

    public int maxResultsOrDefault() {
        return maxResults == null ? 8 : maxResults;
    }
}
