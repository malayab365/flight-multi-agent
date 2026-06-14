package com.flightagent.agent.specialist;

import com.flightagent.tools.BookingToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BookingSpecialist extends AbstractSpecialistAgent {

    private static final String SYSTEM_PROMPT = """
            You are a booking specialist. You create, look up, cancel, and modify \
            flight bookings. Confirm key details (passenger, route, date, price) \
            before booking, and always report the booking_id and PNR.
            """;

    private final BookingToolService booking;

    public BookingSpecialist(ChatClient.Builder builder, BookingToolService booking) {
        super(builder);
        this.booking = booking;
    }

    @Override
    public String name() {
        return "booking_agent";
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected Object[] tools() {
        return new Object[] { this };
    }

    @Tool(description = "Create a flight booking for an offer.")
    public Map<String, Object> bookFlight(
            @ToolParam(description = "Offer ID returned by searchFlights.") String offerId,
            @ToolParam(description = "Passenger full name.") String passengerName,
            @ToolParam(description = "Passenger email for confirmation.") String passengerEmail,
            @ToolParam(description = "Origin IATA code.") String origin,
            @ToolParam(description = "Destination IATA code.") String destination,
            @ToolParam(description = "Departure date YYYY-MM-DD.") String departureDate,
            @ToolParam(description = "Total fare, e.g. '245.30'.") String priceTotal,
            @ToolParam(required = false, description = "Currency code (default USD).") String currency
    ) {
        return booking.bookFlight(offerId, passengerName, passengerEmail,
                origin, destination, departureDate, priceTotal,
                currency == null ? "USD" : currency);
    }

    @Tool(description = "Retrieve a booking by ID.")
    public Map<String, Object> getBooking(
            @ToolParam(description = "Booking ID, e.g. 'BK-...'.") String bookingId
    ) {
        return booking.getBooking(bookingId);
    }

    @Tool(description = "Cancel a booking (refund 80%).")
    public Map<String, Object> cancelBooking(
            @ToolParam(description = "Booking ID.") String bookingId,
            @ToolParam(required = false, description = "Optional cancellation reason.") String reason
    ) {
        return booking.cancelBooking(bookingId, reason);
    }

    @Tool(description = "Modify a booking's date and/or destination (incurs a change fee).")
    public Map<String, Object> modifyBooking(
            @ToolParam(description = "Booking ID.") String bookingId,
            @ToolParam(required = false, description = "New departure date YYYY-MM-DD.") String newDepartureDate,
            @ToolParam(required = false, description = "New destination IATA code.") String newDestination
    ) {
        return booking.modifyBooking(bookingId, newDepartureDate, newDestination);
    }
}
