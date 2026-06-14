package com.flightagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.amadeus.client-id=",
        "app.amadeus.client-secret=",
        "app.weather.api-key="
})
class FlightAgentApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts without errors
    }
}
