package com.flightagent.controller;

import com.flightagent.tools.SearchToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SearchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean SearchToolService searchToolService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void postSearchReturnsOffers() throws Exception {
        when(searchToolService.searchFlights(
                eq("JFK"), eq("LHR"), eq("2026-08-01"),
                isNull(), eq(1), eq("ECONOMY"), eq(5)))
                .thenReturn(Map.of(
                        "source", "sample_data",
                        "offers", List.of(Map.of("offer_id", "SAMPLE-1"))));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"JFK","destination":"LHR","departure_date":"2026-08-01",
                                "adults":1,"travel_class":"ECONOMY","max_results":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("sample_data"))
                .andExpect(jsonPath("$.offers[0].offer_id").value("SAMPLE-1"));
    }

    @Test
    void getAirportCodeReturnsMatches() throws Exception {
        when(searchToolService.getAirportCode("London"))
                .thenReturn(Map.of("source", "sample_data",
                        "matches", List.of(Map.of("name", "London", "iata", "LON"))));

        mockMvc.perform(get("/api/airport-code").param("city", "London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].iata").value("LON"));
    }
}
