package com.flightagent.controller;

import com.flightagent.tools.AncillaryToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AncillaryControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean AncillaryToolService ancillaryToolService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void getSeatMapReturnsFees() throws Exception {
        when(ancillaryToolService.getSeatMap("SAMPLE-1"))
                .thenReturn(Map.of("offer_id", "SAMPLE-1",
                        "currency", "USD",
                        "seat_types", Map.of("WINDOW", 18.0)));

        mockMvc.perform(get("/api/seat-map/SAMPLE-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seat_types.WINDOW").value(18.0));
    }

    @Test
    void postSeatAssignsSeat() throws Exception {
        when(ancillaryToolService.selectSeat(eq("BK-1"), eq("WINDOW"), eq("14A")))
                .thenReturn(Map.of("booking_id", "BK-1",
                        "seat", Map.of("type", "WINDOW", "number", "14A", "fee", 18.0),
                        "status", "SEAT_ASSIGNED"));

        mockMvc.perform(post("/api/booking/BK-1/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seat_type":"WINDOW","seat_number":"14A"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEAT_ASSIGNED"))
                .andExpect(jsonPath("$.seat.number").value("14A"));
    }

    @Test
    void postBaggageAddsItem() throws Exception {
        when(ancillaryToolService.addBaggage(eq("BK-1"), eq("CHECKED_23KG"), eq(2)))
                .thenReturn(Map.of("booking_id", "BK-1",
                        "added", Map.of("type", "CHECKED_23KG", "quantity", 2, "fee", 70.0),
                        "status", "BAGGAGE_ADDED"));

        mockMvc.perform(post("/api/booking/BK-1/baggage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baggage_type":"CHECKED_23KG","quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BAGGAGE_ADDED"))
                .andExpect(jsonPath("$.added.fee").value(70.0));
    }
}
