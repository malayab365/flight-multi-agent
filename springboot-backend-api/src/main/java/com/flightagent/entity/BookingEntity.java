package com.flightagent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class BookingEntity {

    @Id
    @Column(name = "booking_id", length = 20)
    private String bookingId;

    private String offerId;
    private String pnr;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Embedded
    private PassengerInfo passenger;

    @Embedded
    private ItineraryInfo itinerary;

    @Embedded
    private PriceInfo price;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "seat_id")
    private SeatAssignmentEntity seat;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "booking_id")
    private List<BaggageItemEntity> baggage = new ArrayList<>();

    private String cancelReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Version
    private Long version;
}
