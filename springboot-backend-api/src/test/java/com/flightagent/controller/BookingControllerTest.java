package com.flightagent.controller;

import com.flightagent.exception.GlobalExceptionHandler;
import com.flightagent.tools.BookingToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean BookingToolService bookingToolService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void postBookCreatesReservation() throws Exception {
        when(bookingToolService.bookFlight(
                eq("SAMPLE-1"), eq("Ada"), eq("ada@x.com"),
                eq("JFK"), eq("LHR"), eq("2026-08-01"),
                eq("245.30"), eq("USD")))
                .thenReturn(Map.of(
                        "booking_id", "BK-ABCD1234",
                        "pnr", "ZZZ123",
                        "status", "CONFIRMED",
                        "summary", "JFK->LHR on 2026-08-01 for Ada"));

        mockMvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offer_id":"SAMPLE-1","passenger_name":"Ada",
                                "passenger_email":"ada@x.com","origin":"JFK",
                                "destination":"LHR","departure_date":"2026-08-01",
                                "price_total":"245.30","currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking_id").value("BK-ABCD1234"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getBookingReturns404WhenMissing() throws Exception {
        when(bookingToolService.getBooking("BK-MISSING"))
                .thenReturn(Map.of("error", "No booking found with id BK-MISSING"));

        mockMvc.perform(get("/api/booking/BK-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No booking found with id BK-MISSING"));
    }

    @Test
    void getBookingReturnsRecord() throws Exception {
        when(bookingToolService.getBooking("BK-OK"))
                .thenReturn(Map.of("booking_id", "BK-OK", "status", "CONFIRMED"));

        mockMvc.perform(get("/api/booking/BK-OK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking_id").value("BK-OK"));
    }

    @Test
    void postCancelReturnsRefund() throws Exception {
        when(bookingToolService.cancelBooking(eq("BK-1"), any()))
                .thenReturn(Map.of(
                        "booking_id", "BK-1",
                        "status", "CANCELLED",
                        "refund_estimate", Map.of("amount", 196.24, "currency", "USD")));

        mockMvc.perform(post("/api/booking/BK-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"changed plans"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refund_estimate.amount").value(196.24));
    }

    @Test
    void postModifyUpdatesItinerary() throws Exception {
        when(bookingToolService.modifyBooking(eq("BK-1"), eq("2026-09-15"), isNull()))
                .thenReturn(Map.of(
                        "booking_id", "BK-1",
                        "status", "MODIFIED",
                        "changes", Map.of("departure_date", "2026-09-15")));

        mockMvc.perform(post("/api/booking/BK-1/modify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"new_departure_date":"2026-09-15"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MODIFIED"))
                .andExpect(jsonPath("$.changes.departure_date").value("2026-09-15"));
    }
}
