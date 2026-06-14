package com.flightagent.agent.specialist;

import com.flightagent.tools.AncillaryToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AncillarySpecialist extends AbstractSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
            You handle seat selection and baggage add-ons for existing bookings. \
            Show the seat map / fees when helpful and confirm the booking_id before \
            making changes.
            """;

    private final AncillaryToolService ancillary;

    public AncillarySpecialist(ChatClient.Builder builder, AncillaryToolService ancillary) {
        super(builder);
        this.ancillary = ancillary;
    }

    @Override
    public String name() {
        return "ancillary_agent";
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected Object[] tools() {
        return new Object[] { this };
    }

    @Tool(description = "Get the seat map and per-type fees for an offer.")
    public Map<String, Object> getSeatMap(
            @ToolParam(description = "Offer ID.") String offerId
    ) {
        return ancillary.getSeatMap(offerId);
    }

    @Tool(description = "Assign a seat to a booking.")
    public Map<String, Object> selectSeat(
            @ToolParam(description = "Booking ID.") String bookingId,
            @ToolParam(description = "Seat type: WINDOW, AISLE, MIDDLE, EXIT_ROW, or BULKHEAD.") String seatType,
            @ToolParam(description = "Seat number, e.g. '14A'.") String seatNumber
    ) {
        return ancillary.selectSeat(bookingId, seatType, seatNumber);
    }

    @Tool(description = "Add baggage to a booking.")
    public Map<String, Object> addBaggage(
            @ToolParam(description = "Booking ID.") String bookingId,
            @ToolParam(description = "Baggage type: CHECKED_23KG, CHECKED_32KG, EXTRA_CARRYON, or SPORTS_EQUIPMENT.") String baggageType,
            @ToolParam(required = false, description = "Quantity (default 1).") Integer quantity
    ) {
        return ancillary.addBaggage(bookingId, baggageType, quantity == null ? 1 : quantity);
    }
}
