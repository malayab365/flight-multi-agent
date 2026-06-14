package com.flightagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfig(
        Amadeus amadeus,
        Weather weather,
        Cors cors,
        Agent agent
) {
    public record Amadeus(String clientId, String clientSecret, String hostname) {
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }

    public record Weather(String apiKey) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Cors(String allowedOrigins) {}

    public record Agent(int recursionLimit) {}
}
