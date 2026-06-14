package com.flightagent.tools;

import com.flightagent.external.OpenWeatherClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WeatherToolService {

    private static final Map<String, Map<String, String>> CITY_INFO;

    static {
        Map<String, Map<String, String>> info = new LinkedHashMap<>();
        info.put("tokyo", Map.of(
                "country", "Japan",
                "currency", "JPY",
                "timezone", "JST (UTC+9)",
                "language", "Japanese",
                "tip", "Carry cash; many places are card-shy."));
        info.put("london", Map.of(
                "country", "UK",
                "currency", "GBP",
                "timezone", "GMT/BST",
                "language", "English",
                "tip", "Get an Oyster card for the Tube."));
        info.put("paris", Map.of(
                "country", "France",
                "currency", "EUR",
                "timezone", "CET",
                "language", "French",
                "tip", "Many museums close on Mondays."));
        CITY_INFO = Map.copyOf(info);
    }

    private final OpenWeatherClient openWeather;

    public WeatherToolService(OpenWeatherClient openWeather) {
        this.openWeather = openWeather;
    }

    public Map<String, Object> getDestinationWeather(String cityName) {
        if (!openWeather.isConfigured()) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("source", "sample_data");
            out.put("city", cityName);
            out.put("temp_c", 21.0);
            out.put("conditions", "Partly cloudy");
            out.put("humidity", 60);
            return out;
        }
        try {
            Map<String, Object> raw = openWeather.currentWeather(cityName);
            @SuppressWarnings("unchecked")
            Map<String, Object> main = (Map<String, Object>) raw.getOrDefault("main", Map.of());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> weather = (List<Map<String, Object>>) raw.getOrDefault("weather", List.of());
            String description = weather.isEmpty() ? "" : String.valueOf(weather.get(0).get("description"));

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("source", "openweathermap");
            out.put("city", cityName);
            out.put("temp_c", main.get("temp"));
            out.put("conditions", description);
            out.put("humidity", main.get("humidity"));
            return out;
        } catch (Exception exc) {
            log.warn("Weather lookup failed", exc);
            return Map.of("error", "Weather lookup failed: " + exc.getMessage());
        }
    }

    public Map<String, Object> getDestinationInfo(String cityName) {
        String key = cityName == null ? "" : cityName.strip().toLowerCase();
        Map<String, String> info = CITY_INFO.get(key);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("city", cityName);
        out.put("info", info == null
                ? Map.of("note", "No cached info; wire to a travel API.")
                : info);
        return out;
    }
}
