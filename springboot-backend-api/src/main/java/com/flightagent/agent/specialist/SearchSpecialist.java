package com.flightagent.agent.specialist;

import com.flightagent.tools.SearchToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchSpecialist extends AbstractSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
            You are a flight search specialist. Use the search tools to find flights \
            and resolve city names to IATA codes. Always present offers with their \
            offer_id, times, stops, and price so the user (or another agent) can book them.
            """;

    private final SearchToolService search;

    public SearchSpecialist(ChatClient.Builder builder, SearchToolService search) {
        super(builder);
        this.search = search;
    }

    @Override
    public String name() {
        return "search_agent";
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected Object[] tools() {
        return new Object[] { this };
    }

    @Tool(description = "Search for available flights between two airports. Dates are YYYY-MM-DD.")
    public Map<String, Object> searchFlights(
            @ToolParam(description = "Origin IATA airport code, e.g. 'JFK'.") String origin,
            @ToolParam(description = "Destination IATA airport code, e.g. 'LHR'.") String destination,
            @ToolParam(description = "Departure date in YYYY-MM-DD.") String departureDate,
            @ToolParam(required = false, description = "Optional return date in YYYY-MM-DD.") String returnDate,
            @ToolParam(required = false, description = "Number of adult passengers (default 1).") Integer adults,
            @ToolParam(required = false, description = "ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST.") String travelClass,
            @ToolParam(required = false, description = "Maximum number of offers to return (default 5).") Integer maxResults
    ) {
        return search.searchFlights(
                origin, destination, departureDate, returnDate,
                adults == null ? 1 : adults,
                travelClass == null ? "ECONOMY" : travelClass,
                maxResults == null ? 5 : maxResults);
    }

    @Tool(description = "Resolve a city or airport name to its IATA code.")
    public Map<String, Object> getAirportCode(
            @ToolParam(description = "City or airport name, e.g. 'London' or 'Heathrow'.") String cityName
    ) {
        return search.getAirportCode(cityName);
    }
}
