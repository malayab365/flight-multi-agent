package com.flightagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PriceToolServiceTest {

    @Autowired
    PriceToolService priceToolService;

    @Test
    void trackThenCheckPriceWatch() {
        Map<String, Object> watch = priceToolService.trackPrice(
                "JFK", "LAX", "2026-07-01", 150.0, "u@example.com");
        assertThat(watch.get("status")).isEqualTo("ACTIVE");

        String watchId = (String) watch.get("watch_id");
        assertThat(watchId).startsWith("PW-");

        Map<String, Object> check = priceToolService.checkPriceWatch(watchId);
        assertThat(check).containsKey("current_price");
        assertThat(check.get("route")).isEqualTo("JFK-LAX");
    }

    @Test
    void priceHistoryReturnsQuartiles() {
        Map<String, Object> history = priceToolService.getPriceHistory("JFK", "LHR", "2026-08-01");
        assertThat(history).containsKey("quartiles");
        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) history.get("quartiles");
        assertThat(q).containsKeys("minimum", "first", "median", "third", "maximum");
    }

    @Test
    void checkWatchReturnsErrorWhenMissing() {
        Map<String, Object> result = priceToolService.checkPriceWatch("PW-NONEXISTENT");
        assertThat(result.get("error")).asString().contains("No price watch");
    }
}
