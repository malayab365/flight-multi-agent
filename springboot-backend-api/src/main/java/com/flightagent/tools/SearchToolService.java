package com.flightagent.tools;

import com.flightagent.external.AmadeusClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SearchToolService {

    private final AmadeusClient amadeus;

    private static final Map<String, String> COMMON_AIRPORT_CODES = Map.of(
            "new york", "NYC",
            "london", "LON",
            "paris", "PAR",
            "tokyo", "TYO",
            "delhi", "DEL",
            "dubai", "DXB",
            "san francisco", "SFO",
            "los angeles", "LAX"
    );

    public SearchToolService(AmadeusClient amadeus) {
        this.amadeus = amadeus;
    }

    public Map<String, Object> searchFlights(
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            int adults,
            String travelClass,
            int maxResults) {

        if (!amadeus.isConfigured()) {
            List<Map<String, Object>> offers = sampleOffers(origin, destination, departureDate);
            if (maxResults > 0 && offers.size() > maxResults) {
                offers = offers.subList(0, maxResults);
            }
            return Map.of("source", "sample_data", "offers", offers);
        }
        try {
            Map<String, Object> raw = amadeus.flightOffersSearch(
                    origin, destination, departureDate, returnDate,
                    adults, travelClass, maxResults);
            List<Map<String, Object>> offers = amadeus.extractOffers(raw, travelClass, departureDate);
            return Map.of("source", "amadeus", "offers", offers);
        } catch (Exception exc) {
            log.warn("Amadeus search failed", exc);
            return Map.of("error", "Amadeus search failed: " + exc.getMessage());
        }
    }

    public Map<String, Object> getAirportCode(String cityName) {
        if (!amadeus.isConfigured()) {
            String code = COMMON_AIRPORT_CODES.get(cityName.strip().toLowerCase());
            List<Map<String, Object>> matches = code == null
                    ? List.of()
                    : List.of(Map.of("name", cityName, "iata", code));
            return Map.of("source", "sample_data", "matches", matches);
        }
        try {
            Map<String, Object> raw = amadeus.referenceDataLocations(cityName);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) raw.getOrDefault("data", List.of());
            List<Map<String, Object>> matches = data.stream().map(d -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> address = (Map<String, Object>) d.getOrDefault("address", Map.of());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", d.get("name"));
                m.put("iata", d.get("iataCode"));
                m.put("country", address.get("countryCode"));
                return m;
            }).toList();
            return Map.of("source", "amadeus", "matches", matches);
        } catch (Exception exc) {
            log.warn("Amadeus location lookup failed", exc);
            return Map.of("error", "Amadeus location lookup failed: " + exc.getMessage());
        }
    }

    private List<Map<String, Object>> sampleOffers(String origin, String destination, String departureDate) {
        return List.of(
                offerMap("SAMPLE-1", "AA", "AA100", origin, destination, departureDate,
                        "08:30", "11:45", 0, "ECONOMY", "245.30", "USD", 7),
                offerMap("SAMPLE-2", "DL", "DL205", origin, destination, departureDate,
                        "13:10", "17:55", 1, "ECONOMY", "198.00", "USD", 3)
        );
    }

    private Map<String, Object> offerMap(
            String offerId, String airline, String flightNumber,
            String origin, String destination, String departureDate,
            String departTime, String arriveTime, int stops, String cabin,
            String priceTotal, String currency, int seatsRemaining) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("offer_id", offerId);
        m.put("airline", airline);
        m.put("flight_number", flightNumber);
        m.put("origin", origin);
        m.put("destination", destination);
        m.put("departure_date", departureDate);
        m.put("depart_time", departTime);
        m.put("arrive_time", arriveTime);
        m.put("stops", stops);
        m.put("cabin", cabin);
        m.put("price", Map.of("total", priceTotal, "currency", currency));
        m.put("seats_remaining", seatsRemaining);
        return m;
    }
}
