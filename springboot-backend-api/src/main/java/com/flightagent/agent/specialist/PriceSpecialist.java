package com.flightagent.agent.specialist;

import com.flightagent.tools.PriceToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PriceSpecialist extends AbstractSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
            You are a fare-tracking specialist. You create price watches, check \
            them, and provide historical price context to advise on whether a fare \
            is a good deal.
            """;

    private final PriceToolService price;

    public PriceSpecialist(ChatClient.Builder builder, PriceToolService price) {
        super(builder);
        this.price = price;
    }

    @Override
    public String name() {
        return "price_agent";
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected Object[] tools() {
        return new Object[] { this };
    }

    @Tool(description = "Start tracking a fare and alert the user when it drops to a target price.")
    public Map<String, Object> trackPrice(
            @ToolParam(description = "Origin IATA code.") String origin,
            @ToolParam(description = "Destination IATA code.") String destination,
            @ToolParam(description = "Departure date YYYY-MM-DD.") String departureDate,
            @ToolParam(description = "Target price in USD, e.g. 350.0.") Double targetPrice,
            @ToolParam(description = "Email to notify when target is met.") String email
    ) {
        return price.trackPrice(origin, destination, departureDate, targetPrice, email);
    }

    @Tool(description = "Check the current price for an active price watch.")
    public Map<String, Object> checkPriceWatch(
            @ToolParam(description = "Watch ID returned by trackPrice.") String watchId
    ) {
        return price.checkPriceWatch(watchId);
    }

    @Tool(description = "Get the historical price distribution for a route.")
    public Map<String, Object> getPriceHistory(
            @ToolParam(description = "Origin IATA code.") String origin,
            @ToolParam(description = "Destination IATA code.") String destination,
            @ToolParam(description = "Departure date YYYY-MM-DD.") String departureDate
    ) {
        return price.getPriceHistory(origin, destination, departureDate);
    }
}
