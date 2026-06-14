package com.flightagent.controller;

import com.flightagent.tools.PriceToolService;
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
class PriceControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean PriceToolService priceToolService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void postTrackCreatesWatch() throws Exception {
        when(priceToolService.trackPrice(
                eq("JFK"), eq("LAX"), eq("2026-07-01"),
                eq(150.0), eq("u@x.com")))
                .thenReturn(Map.of(
                        "watch_id", "PW-ABCD1234",
                        "status", "ACTIVE",
                        "message", "Tracking JFK->LAX"));

        mockMvc.perform(post("/api/price/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"JFK","destination":"LAX",
                                "departure_date":"2026-07-01",
                                "target_price":150.0,"email":"u@x.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watch_id").value("PW-ABCD1234"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getWatchReturnsCurrentPrice() throws Exception {
        when(priceToolService.checkPriceWatch("PW-1"))
                .thenReturn(Map.of(
                        "watch_id", "PW-1",
                        "route", "JFK-LAX",
                        "current_price", 142.50,
                        "target_price", 150.0,
                        "alert_triggered", true,
                        "action", "Notify user"));

        mockMvc.perform(get("/api/price/watch/PW-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_price").value(142.50))
                .andExpect(jsonPath("$.alert_triggered").value(true));
    }

    @Test
    void getHistoryReturnsQuartiles() throws Exception {
        when(priceToolService.getPriceHistory("JFK", "LHR", "2026-08-01"))
                .thenReturn(Map.of(
                        "route", "JFK-LHR",
                        "departure_date", "2026-08-01",
                        "currency", "USD",
                        "quartiles", Map.of("minimum", 140.0, "first", 170.0,
                                "median", 200.0, "third", 240.0, "maximum", 320.0)));

        mockMvc.perform(get("/api/price/history")
                        .param("origin", "JFK")
                        .param("destination", "LHR")
                        .param("departure_date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quartiles.median").value(200.0));
    }
}
