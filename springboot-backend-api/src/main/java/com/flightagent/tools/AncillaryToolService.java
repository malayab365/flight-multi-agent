package com.flightagent.tools;

import com.flightagent.entity.BaggageItemEntity;
import com.flightagent.entity.BookingEntity;
import com.flightagent.entity.SeatAssignmentEntity;
import com.flightagent.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AncillaryToolService {

    private static final Map<String, BigDecimal> SEAT_FEES;
    private static final Map<String, BigDecimal> BAGGAGE_FEES;

    static {
        LinkedHashMap<String, BigDecimal> seats = new LinkedHashMap<>();
        seats.put("WINDOW", new BigDecimal("18.00"));
        seats.put("AISLE", new BigDecimal("15.00"));
        seats.put("MIDDLE", new BigDecimal("0.00"));
        seats.put("EXIT_ROW", new BigDecimal("39.00"));
        seats.put("BULKHEAD", new BigDecimal("29.00"));
        SEAT_FEES = Map.copyOf(seats);

        LinkedHashMap<String, BigDecimal> bags = new LinkedHashMap<>();
        bags.put("CHECKED_23KG", new BigDecimal("35.00"));
        bags.put("CHECKED_32KG", new BigDecimal("60.00"));
        bags.put("EXTRA_CARRYON", new BigDecimal("25.00"));
        bags.put("SPORTS_EQUIPMENT", new BigDecimal("70.00"));
        BAGGAGE_FEES = Map.copyOf(bags);
    }

    private final BookingRepository bookingRepository;

    public AncillaryToolService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Map<String, Object> getSeatMap(String offerId) {
        Map<String, Object> seatTypes = new LinkedHashMap<>();
        SEAT_FEES.forEach((k, v) -> seatTypes.put(k, v));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("offer_id", offerId);
        out.put("currency", "USD");
        out.put("seat_types", seatTypes);
        return out;
    }

    @Transactional
    public Map<String, Object> selectSeat(String bookingId, String seatType, String seatNumber) {
        Optional<BookingEntity> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Map.of("error", "No booking found with id " + bookingId);
        }
        String upperType = seatType == null ? "" : seatType.toUpperCase();
        if (!SEAT_FEES.containsKey(upperType)) {
            return Map.of("error", "Unknown seat_type. Choose from " + List.copyOf(SEAT_FEES.keySet()));
        }
        BookingEntity booking = opt.get();
        BigDecimal fee = SEAT_FEES.get(upperType);
        booking.setSeat(new SeatAssignmentEntity(upperType, seatNumber, fee));
        bookingRepository.save(booking);

        Map<String, Object> seat = new LinkedHashMap<>();
        seat.put("type", upperType);
        seat.put("number", seatNumber);
        seat.put("fee", fee);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", bookingId);
        out.put("seat", seat);
        out.put("status", "SEAT_ASSIGNED");
        return out;
    }

    @Transactional
    public Map<String, Object> addBaggage(String bookingId, String baggageType, int quantity) {
        Optional<BookingEntity> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Map.of("error", "No booking found with id " + bookingId);
        }
        String upperType = baggageType == null ? "" : baggageType.toUpperCase();
        if (!BAGGAGE_FEES.containsKey(upperType)) {
            return Map.of("error", "Unknown baggage_type. Choose from " + List.copyOf(BAGGAGE_FEES.keySet()));
        }
        BookingEntity booking = opt.get();
        int qty = Math.max(1, quantity);
        BigDecimal fee = BAGGAGE_FEES.get(upperType)
                .multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);

        BaggageItemEntity item = new BaggageItemEntity(booking.getBookingId(), upperType, qty, fee);
        booking.getBaggage().add(item);
        bookingRepository.save(booking);

        Map<String, Object> added = new LinkedHashMap<>();
        added.put("type", upperType);
        added.put("quantity", qty);
        added.put("fee", fee);

        List<Map<String, Object>> all = booking.getBaggage().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", b.getBaggageType());
            m.put("quantity", b.getQuantity());
            m.put("fee", b.getFee());
            return m;
        }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("booking_id", bookingId);
        out.put("added", added);
        out.put("all_baggage", all);
        out.put("status", "BAGGAGE_ADDED");
        return out;
    }
}
