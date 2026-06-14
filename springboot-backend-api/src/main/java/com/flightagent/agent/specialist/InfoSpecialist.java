package com.flightagent.agent.specialist;

import com.flightagent.tools.WeatherToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InfoSpecialist extends AbstractSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
            You provide destination weather and practical travel information to help \
            the traveler plan their trip.
            """;

    private final WeatherToolService weather;

    public InfoSpecialist(ChatClient.Builder builder, WeatherToolService weather) {
        super(builder);
        this.weather = weather;
    }

    @Override
    public String name() {
        return "info_agent";
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected Object[] tools() {
        return new Object[] { this };
    }

    @Tool(description = "Get current weather for a destination city.")
    public Map<String, Object> getDestinationWeather(
            @ToolParam(description = "City name, e.g. 'Tokyo'.") String cityName
    ) {
        return weather.getDestinationWeather(cityName);
    }

    @Tool(description = "Get practical travel info (currency, timezone, language, tips) for a city.")
    public Map<String, Object> getDestinationInfo(
            @ToolParam(description = "City name, e.g. 'Paris'.") String cityName
    ) {
        return weather.getDestinationInfo(cityName);
    }
}
