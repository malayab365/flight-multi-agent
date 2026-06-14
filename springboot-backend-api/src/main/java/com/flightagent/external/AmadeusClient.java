package com.flightagent.external;

import com.flightagent.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class AmadeusClient {

    private final AppConfig appConfig;
    private final WebClient webClient;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public AmadeusClient(AppConfig appConfig, WebClient.Builder webClientBuilder) {
        this.appConfig = appConfig;
        String host = Optional.ofNullable(appConfig.amadeus().hostname())
                .filter(h -> !h.isBlank()).orElse("test");
        String base = "production".equalsIgnoreCase(host)
                ? "https://api.amadeus.com"
                : "https://test.api.amadeus.com";
        this.webClient = webClientBuilder.baseUrl(base).build();
    }

    public boolean isConfigured() {
        return appConfig.amadeus().isConfigured();
    }

    @SuppressWarnings("unchecked")
    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        Map<String, Object> resp = webClient.post()
                .uri("/v1/security/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", appConfig.amadeus().clientId())
                        .with("client_secret", appConfig.amadeus().clientSecret()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null || resp.get("access_token") == null) {
            throw new IllegalStateException("Amadeus token response missing access_token");
        }
        cachedToken = (String) resp.get("access_token");
        int expiresIn = ((Number) resp.getOrDefault("expires_in", 1700)).intValue();
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
        return cachedToken;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> flightOffersSearch(
            String origin, String destination, String departureDate,
            String returnDate, int adults, String travelClass, int maxResults) {

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departureDate)
                            .queryParam("adults", adults)
                            .queryParam("travelClass", travelClass)
                            .queryParam("currencyCode", "USD")
                            .queryParam("max", maxResults);
                    if (returnDate != null && !returnDate.isBlank()) {
                        uriBuilder.queryParam("returnDate", returnDate);
                    }
                    return uriBuilder.build();
                })
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> referenceDataLocations(String keyword) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/reference-data/locations")
                        .queryParam("keyword", keyword)
                        .queryParam("subType", "CITY,AIRPORT")
                        .build())
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public List<Map<String, Object>> extractOffers(Map<String, Object> raw, String travelClass, String departureDate) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) raw.getOrDefault("data", List.of());
        return data.stream().map(o -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itineraries = (List<Map<String, Object>>) o.get("itineraries");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) itineraries.get(0).get("segments");
            Map<String, Object> first = segments.get(0);
            Map<String, Object> last = segments.get(segments.size() - 1);

            @SuppressWarnings("unchecked")
            Map<String, Object> firstDep = (Map<String, Object>) first.get("departure");
            @SuppressWarnings("unchecked")
            Map<String, Object> lastArr = (Map<String, Object>) last.get("arrival");
            @SuppressWarnings("unchecked")
            Map<String, Object> price = (Map<String, Object>) o.get("price");

            Map<String, Object> offer = new HashMap<>();
            offer.put("offer_id", o.get("id"));
            offer.put("airline", first.get("carrierCode"));
            offer.put("flight_number", String.valueOf(first.get("carrierCode")) + first.get("number"));
            offer.put("origin", firstDep.get("iataCode"));
            offer.put("destination", lastArr.get("iataCode"));
            offer.put("departure_date", departureDate);
            offer.put("depart_time", firstDep.get("at"));
            offer.put("arrive_time", lastArr.get("at"));
            offer.put("stops", segments.size() - 1);
            offer.put("cabin", travelClass);
            offer.put("price", Map.of(
                    "total", price.get("total"),
                    "currency", price.get("currency")));
            offer.put("seats_remaining", o.get("numberOfBookableSeats"));
            return offer;
        }).toList();
    }
}
