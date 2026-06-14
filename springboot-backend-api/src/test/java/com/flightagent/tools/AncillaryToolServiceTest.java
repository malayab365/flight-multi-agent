package com.flightagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AncillaryToolServiceTest {

    @Autowired
    AncillaryToolService ancillaryToolService;

    @Autowired
    BookingToolService bookingToolService;

    @Test
    void seatAndBaggageAreAssigned() {
        Map<String, Object> booked = bookingToolService.bookFlight(
                "SAMPLE-2", "Alan Turing", "alan@example.com",
                "SFO", "NRT", "2026-09-10",
                "780.00", "USD");
        String bookingId = (String) booked.get("booking_id");

        Map<String, Object> seat = ancillaryToolService.selectSeat(bookingId, "WINDOW", "12A");
        assertThat(seat.get("status")).isEqualTo("SEAT_ASSIGNED");

        Map<String, Object> bag = ancillaryToolService.addBaggage(bookingId, "CHECKED_23KG", 2);
        assertThat(bag.get("status")).isEqualTo("BAGGAGE_ADDED");

        @SuppressWarnings("unchecked")
        Map<String, Object> added = (Map<String, Object>) bag.get("added");
        assertThat((BigDecimal) added.get("fee")).isEqualByComparingTo("70.00");
    }

    @Test
    void seatMapListsAllTypes() {
        Map<String, Object> map = ancillaryToolService.getSeatMap("SAMPLE-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> seatTypes = (Map<String, Object>) map.get("seat_types");
        assertThat(seatTypes).containsKeys("WINDOW", "AISLE", "MIDDLE", "EXIT_ROW", "BULKHEAD");
    }

    @Test
    void unknownSeatTypeReturnsError() {
        Map<String, Object> booked = bookingToolService.bookFlight(
                "SAMPLE-1", "Test", "t@t.com",
                "JFK", "LAX", "2026-08-01",
                "200.00", "USD");
        String bookingId = (String) booked.get("booking_id");

        Map<String, Object> seat = ancillaryToolService.selectSeat(bookingId, "PRIVATE_JET", null);
        assertThat(seat.get("error")).asString().contains("Unknown seat_type");
    }
}
