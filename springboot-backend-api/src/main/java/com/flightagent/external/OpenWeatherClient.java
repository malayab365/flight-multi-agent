package com.flightagent.external;

import com.flightagent.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class OpenWeatherClient {

    private final AppConfig appConfig;
    private final WebClient webClient;

    public OpenWeatherClient(AppConfig appConfig, WebClient.Builder webClientBuilder) {
        this.appConfig = appConfig;
        this.webClient = webClientBuilder
                .baseUrl("https://api.openweathermap.org")
                .build();
    }

    public boolean isConfigured() {
        return appConfig.weather().isConfigured();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> currentWeather(String cityName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/data/2.5/weather")
                        .queryParam("q", cityName)
                        .queryParam("appid", appConfig.weather().apiKey())
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
