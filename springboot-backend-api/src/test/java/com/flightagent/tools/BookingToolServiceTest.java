package com.flightagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BookingToolServiceTest {

    @Autowired
    BookingToolService bookingToolService;

    @Test
    void bookThenCancel() {
        Map<String, Object> booked = bookingToolService.bookFlight(
                "SAMPLE-1", "Ada Lovelace", "ada@example.com",
                "JFK", "LHR", "2026-08-01",
                "245.30", "USD");

        assertThat(booked.get("status")).isEqualTo("CONFIRMED");
        String bookingId = (String) booked.get("booking_id");
        assertThat(bookingId).startsWith("BK-");

        Map<String, Object> cancelled = bookingToolService.cancelBooking(bookingId, "test");
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");

        @SuppressWarnings("unchecked")
        Map<String, Object> refund = (Map<String, Object>) cancelled.get("refund_estimate");
        BigDecimal amount = (BigDecimal) refund.get("amount");
        assertThat(amount).isEqualByComparingTo(new BigDecimal("196.24"));
    }

    @Test
    void getBookingReturnsErrorWhenMissing() {
        Map<String, Object> result = bookingToolService.getBooking("BK-NONEXISTENT");
        assertThat(result.get("error")).asString().contains("No booking found");
    }

    @Test
    void modifyBookingChangesItinerary() {
        Map<String, Object> booked = bookingToolService.bookFlight(
                "SAMPLE-2", "Alan Turing", "alan@example.com",
                "SFO", "NRT", "2026-09-10",
                "780.00", "USD");
        String bookingId = (String) booked.get("booking_id");

        Map<String, Object> modified = bookingToolService.modifyBooking(
                bookingId, "2026-09-15", null);
        assertThat(modified.get("status")).isEqualTo("MODIFIED");

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) modified.get("changes");
        assertThat(changes.get("departure_date")).isEqualTo("2026-09-15");
    }
}
