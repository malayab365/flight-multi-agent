package com.flightagent.tools;

import com.flightagent.entity.*;
import com.flightagent.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingToolService {

    private final BookingRepository bookingRepository;
    private final IdGenerator idGenerator;

    public BookingToolService(BookingRepository bookingRepository, IdGenerator idGenerator) {
        this.bookingRepository = bookingRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Map<String, Object> bookFlight(
            String offerId,
            String passengerName,
            String passengerEmail,
            String origin,
            String destination,
            String departureDate,
            String priceTotal,
            String currency) {

        String bookingId = idGenerator.newId("BK");
        String pnr = idGenerator.newId("PNR").substring("PNR-".length());

        BookingEntity booking = new BookingEntity();
        booking.setBookingId(bookingId);
        booking.setOfferId(offerId);
        booking.setPnr(pnr);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassenger(new PassengerInfo(passengerName, passengerEmail));
        booking.setItinerary(new ItineraryInfo(origin, destination, departureDate));
        booking.setPrice(new PriceInfo(new BigDecimal(priceTotal), currency == null ? "USD" : currency));
        booking.setCreatedAt(Instant.now());

        bookingRepository.save(booking);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", bookingId);
        out.put("pnr", pnr);
        out.put("status", "CONFIRMED");
        out.put("summary", origin + "->" + destination + " on " + departureDate + " for " + passengerName);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBooking(String bookingId) {
        Optional<BookingEntity> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Map.of("error", "No booking found with id " + bookingId);
        }
        return toMap(opt.get());
    }

    @Transactional
    public Map<String, Object> cancelBooking(String bookingId, String reason) {
        Optional<BookingEntity> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Map.of("error", "No booking found with id " + bookingId);
        }
        BookingEntity booking = opt.get();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return Map.of(
                    "booking_id", bookingId,
                    "status", "CANCELLED",
                    "note", "Booking was already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(reason);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        BigDecimal refund = booking.getPrice().getPriceTotal()
                .multiply(new BigDecimal("0.80"))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", bookingId);
        out.put("status", "CANCELLED");
        out.put("refund_estimate", Map.of(
                "amount", refund,
                "currency", booking.getPrice().getCurrency()));
        return out;
    }

    @Transactional
    public Map<String, Object> modifyBooking(
            String bookingId, String newDepartureDate, String newDestination) {
        Optional<BookingEntity> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Map.of("error", "No booking found with id " + bookingId);
        }
        BookingEntity booking = opt.get();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return Map.of("error", "Cannot modify a cancelled booking.");
        }

        Map<String, String> changed = new LinkedHashMap<>();
        if (newDepartureDate != null && !newDepartureDate.isBlank()) {
            booking.getItinerary().setDepartureDate(newDepartureDate);
            changed.put("departure_date", newDepartureDate);
        }
        if (newDestination != null && !newDestination.isBlank()) {
            booking.getItinerary().setDestination(newDestination);
            changed.put("destination", newDestination);
        }
        if (changed.isEmpty()) {
            return Map.of("error", "No changes provided.");
        }

        booking.setStatus(BookingStatus.MODIFIED);
        booking.setModifiedAt(Instant.now());
        bookingRepository.save(booking);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", bookingId);
        out.put("status", "MODIFIED");
        out.put("changes", changed);
        out.put("change_fee", Map.of(
                "amount", new BigDecimal("75.00"),
                "currency", booking.getPrice().getCurrency()));
        out.put("itinerary", Map.of(
                "origin", booking.getItinerary().getOrigin(),
                "destination", booking.getItinerary().getDestination(),
                "departure_date", booking.getItinerary().getDepartureDate()));
        return out;
    }

    public Map<String, Object> toMap(BookingEntity b) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", b.getBookingId());
        out.put("offer_id", b.getOfferId());
        out.put("pnr", b.getPnr());
        out.put("status", b.getStatus() == null ? null : b.getStatus().name());
        if (b.getPassenger() != null) {
            out.put("passenger", Map.of(
                    "name", nullSafe(b.getPassenger().getPassengerName()),
                    "email", nullSafe(b.getPassenger().getPassengerEmail())));
        }
        if (b.getItinerary() != null) {
            out.put("itinerary", Map.of(
                    "origin", nullSafe(b.getItinerary().getOrigin()),
                    "destination", nullSafe(b.getItinerary().getDestination()),
                    "departure_date", nullSafe(b.getItinerary().getDepartureDate())));
        }
        if (b.getPrice() != null) {
            out.put("price", Map.of(
                    "total", b.getPrice().getPriceTotal(),
                    "currency", nullSafe(b.getPrice().getCurrency())));
        }
        if (b.getSeat() != null) {
            Map<String, Object> seat = new LinkedHashMap<>();
            seat.put("type", b.getSeat().getSeatType());
            seat.put("number", b.getSeat().getSeatNumber());
            seat.put("fee", b.getSeat().getFee());
            out.put("seat", seat);
        } else {
            out.put("seat", null);
        }
        out.put("baggage", b.getBaggage().stream().map(bag -> {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("type", bag.getBaggageType());
            bm.put("quantity", bag.getQuantity());
            bm.put("fee", bag.getFee());
            return bm;
        }).toList());
        out.put("cancel_reason", b.getCancelReason());
        out.put("created_at", b.getCreatedAt() == null ? null : b.getCreatedAt().toString());
        out.put("cancelled_at", b.getCancelledAt() == null ? null : b.getCancelledAt().toString());
        out.put("modified_at", b.getModifiedAt() == null ? null : b.getModifiedAt().toString());
        return out;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
