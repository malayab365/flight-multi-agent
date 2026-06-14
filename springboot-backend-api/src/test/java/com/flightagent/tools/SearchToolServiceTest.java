package com.flightagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SearchToolServiceTest {

    @Autowired
    SearchToolService searchToolService;

    @Test
    void searchReturnsOffers() {
        Map<String, Object> result = searchToolService.searchFlights(
                "JFK", "LHR", "2026-08-01",
                null, 1, "ECONOMY", 5);

        assertThat(result).containsKey("offers");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> offers = (List<Map<String, Object>>) result.get("offers");
        assertThat(offers).isNotEmpty();
        assertThat(offers.get(0)).containsKeys("offer_id", "airline", "price");
        assertThat(result.get("source")).isEqualTo("sample_data");
    }

    @Test
    void getAirportCodeReturnsKnownCity() {
        Map<String, Object> result = searchToolService.getAirportCode("London");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("matches");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).get("iata")).isEqualTo("LON");
    }
}
