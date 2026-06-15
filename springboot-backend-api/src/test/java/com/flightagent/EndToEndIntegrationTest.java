package com.flightagent;

import com.flightagent.agent.ChatService;
import com.flightagent.dto.response.ChatResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Phase 6 verification — boots the full Spring Boot application against H2
 * with no Amadeus / OpenWeather / OpenRouter keys, exercising the frontend's
 * happy-path flows (search → book → seat → baggage → cancel, price track,
 * weather, chat) end-to-end through real REST controllers and tool services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.amadeus.client-id=",
        "app.amadeus.client-secret=",
        "app.weather.api-key="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockitoBean
    ChatService chatService;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void searchFlightsReturnsSampleOffers() {
        Map<String, Object> body = Map.of(
                "origin", "JFK",
                "destination", "LHR",
                "departure_date", "2026-08-01",
                "adults", 1,
                "travel_class", "ECONOMY",
                "max_results", 5);

        ResponseEntity<Map> resp = rest.postForEntity(url("/api/search"), body, Map.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("source", "sample_data");
        List<Map<String, Object>> offers = (List<Map<String, Object>>) resp.getBody().get("offers");
        assertThat(offers).isNotEmpty();
        assertThat(offers.get(0)).containsKeys("offer_id", "airline", "price");
    }

    @Test
    @Order(2)
    void getAirportCodeResolvesKnownCity() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/api/airport-code?city=London"), Map.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("source", "sample_data");
    }

    @Test
    @Order(3)
    @SuppressWarnings("unchecked")
    void bookCancelAndAncillaryFlow() {
        Map<String, Object> bookBody = Map.of(
                "offer_id", "SAMPLE-1",
                "passenger_name", "Jane Doe",
                "passenger_email", "jane@example.com",
                "origin", "JFK",
                "destination", "LHR",
                "departure_date", "2026-08-01",
                "price_total", "250.00",
                "currency", "USD");
        ResponseEntity<Map> bookResp = rest.postForEntity(url("/api/book"), bookBody, Map.class);
        assertThat(bookResp.getStatusCode().is2xxSuccessful()).isTrue();
        String bookingId = String.valueOf(bookResp.getBody().get("booking_id"));
        assertThat(bookingId).startsWith("BK-");

        ResponseEntity<Map> detail = rest.getForEntity(url("/api/booking/" + bookingId), Map.class);
        assertThat(detail.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(detail.getBody()).containsEntry("status", "CONFIRMED");

        ResponseEntity<Map> seatMap = rest.getForEntity(url("/api/seat-map/SAMPLE-1"), Map.class);
        assertThat(seatMap.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(seatMap.getBody()).containsKeys("offer_id", "seat_types");

        Map<String, Object> seatBody = Map.of("seat_type", "WINDOW", "seat_number", "12A");
        ResponseEntity<Map> seatResp = rest.postForEntity(
                url("/api/booking/" + bookingId + "/seat"), seatBody, Map.class);
        assertThat(seatResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> seat = (Map<String, Object>) seatResp.getBody().get("seat");
        assertThat(seat).containsEntry("type", "WINDOW");

        Map<String, Object> bagBody = Map.of("baggage_type", "CHECKED", "quantity", 1);
        ResponseEntity<Map> bagResp = rest.postForEntity(
                url("/api/booking/" + bookingId + "/baggage"), bagBody, Map.class);
        assertThat(bagResp.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> cancelBody = Map.of("reason", "Plans changed");
        ResponseEntity<Map> cancelResp = rest.postForEntity(
                url("/api/booking/" + bookingId + "/cancel"), cancelBody, Map.class);
        assertThat(cancelResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(cancelResp.getBody()).containsEntry("status", "CANCELLED");
        assertThat(cancelResp.getBody()).containsKey("refund_estimate");
    }

    @Test
    @Order(4)
    void priceTrackAndHistoryFlow() {
        Map<String, Object> trackBody = Map.of(
                "origin", "JFK",
                "destination", "LHR",
                "departure_date", "2026-08-01",
                "target_price", 200,
                "email", "alerts@example.com");
        ResponseEntity<Map> trackResp = rest.postForEntity(url("/api/price/track"), trackBody, Map.class);
        assertThat(trackResp.getStatusCode().is2xxSuccessful()).isTrue();
        String watchId = String.valueOf(trackResp.getBody().get("watch_id"));
        assertThat(watchId).startsWith("PW-");

        ResponseEntity<Map> watchResp = rest.getForEntity(url("/api/price/watch/" + watchId), Map.class);
        assertThat(watchResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(watchResp.getBody()).containsKey("alert_triggered");

        ResponseEntity<Map> historyResp = rest.getForEntity(
                url("/api/price/history?origin=JFK&destination=LHR&departure_date=2026-08-01"),
                Map.class);
        assertThat(historyResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(historyResp.getBody()).containsKey("quartiles");
    }

    @Test
    @Order(5)
    void weatherAndDestinationInfoFallbackToSampleData() {
        ResponseEntity<Map> weather = rest.getForEntity(url("/api/weather/London"), Map.class);
        assertThat(weather.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(weather.getBody()).containsKey("conditions");

        ResponseEntity<Map> info = rest.getForEntity(url("/api/destination/London"), Map.class);
        assertThat(info.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(info.getBody()).containsEntry("city", "London");
    }

    @Test
    @Order(6)
    void chatEndpointRoutesThroughMockedSupervisor() {
        when(chatService.chat(eq("Find flights JFK to LHR on 2026-08-01"), any()))
                .thenReturn(new ChatResponse(
                        "I found two sample offers from JFK to LHR on 2026-08-01.",
                        "search_agent"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(
                "{\"message\":\"Find flights JFK to LHR on 2026-08-01\",\"history\":[]}",
                headers);

        ResponseEntity<Map> resp = rest.exchange(
                url("/api/chat"), HttpMethod.POST, entity, Map.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("agent", "search_agent");
        assertThat(String.valueOf(resp.getBody().get("response"))).contains("JFK to LHR");
    }

    @Test
    @Order(7)
    void missingBookingReturns404() {
        ResponseEntity<Map> resp = rest.getForEntity(url("/api/booking/BK-DOESNOTEXIST"), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).containsKey("detail");
    }
}
