package com.flightagent.persistence;

import com.flightagent.entity.*;
import com.flightagent.repository.BookingRepository;
import com.flightagent.repository.PriceWatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PersistenceSmokeTest {

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    PriceWatchRepository priceWatchRepository;

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    @Test
    void saveAndFindBooking() {
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(newId("BK-"));
        booking.setOfferId("SAMPLE-1");
        booking.setPnr("ABC12345");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassenger(new PassengerInfo("Ada Lovelace", "ada@example.com"));
        booking.setItinerary(new ItineraryInfo("JFK", "LHR", "2026-08-01"));
        booking.setPrice(new PriceInfo(new BigDecimal("245.30"), "USD"));
        booking.setCreatedAt(Instant.now());

        BookingEntity saved = bookingRepository.save(booking);

        Optional<BookingEntity> found = bookingRepository.findById(saved.getBookingId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(found.get().getPassenger().getPassengerName()).isEqualTo("Ada Lovelace");
        assertThat(found.get().getItinerary().getOrigin()).isEqualTo("JFK");
        assertThat(found.get().getPrice().getPriceTotal()).isEqualByComparingTo("245.30");
    }

    @Test
    void bookingWithSeatAndBaggage() {
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(newId("BK-"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassenger(new PassengerInfo("Grace Hopper", "grace@example.com"));
        booking.setItinerary(new ItineraryInfo("LAX", "NRT", "2026-09-15"));
        booking.setPrice(new PriceInfo(new BigDecimal("900.00"), "USD"));
        booking.setCreatedAt(Instant.now());

        SeatAssignmentEntity seat = new SeatAssignmentEntity("WINDOW", "14A", new BigDecimal("18.00"));
        booking.setSeat(seat);

        booking.getBaggage().add(
                new BaggageItemEntity(booking.getBookingId(), "CHECKED_23KG", 2, new BigDecimal("70.00"))
        );

        BookingEntity saved = bookingRepository.save(booking);

        BookingEntity found = bookingRepository.findById(saved.getBookingId()).orElseThrow();
        assertThat(found.getSeat().getSeatType()).isEqualTo("WINDOW");
        assertThat(found.getBaggage()).hasSize(1);
        assertThat(found.getBaggage().get(0).getFee()).isEqualByComparingTo("70.00");
    }

    @Test
    void cancelBookingStatusUpdate() {
        BookingEntity booking = new BookingEntity();
        String id = newId("BK-");
        booking.setBookingId(id);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassenger(new PassengerInfo("Test User", "test@test.com"));
        booking.setItinerary(new ItineraryInfo("ORD", "CDG", "2026-10-01"));
        booking.setPrice(new PriceInfo(new BigDecimal("500.00"), "USD"));
        booking.setCreatedAt(Instant.now());
        bookingRepository.save(booking);

        BookingEntity toCancel = bookingRepository.findById(id).orElseThrow();
        toCancel.setStatus(BookingStatus.CANCELLED);
        toCancel.setCancelledAt(Instant.now());
        bookingRepository.save(toCancel);

        assertThat(bookingRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void saveAndFindPriceWatch() {
        PriceWatchEntity watch = new PriceWatchEntity();
        watch.setWatchId(newId("PW-"));
        watch.setOrigin("JFK");
        watch.setDestination("LAX");
        watch.setDepartureDate("2026-07-01");
        watch.setTargetPrice(new BigDecimal("150.00"));
        watch.setEmail("user@example.com");
        watch.setStatus(WatchStatus.ACTIVE);
        watch.setCreatedAt(Instant.now());

        PriceWatchEntity saved = priceWatchRepository.save(watch);

        Optional<PriceWatchEntity> found = priceWatchRepository.findById(saved.getWatchId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(WatchStatus.ACTIVE);
        assertThat(found.get().getTargetPrice()).isEqualByComparingTo("150.00");
    }
}
