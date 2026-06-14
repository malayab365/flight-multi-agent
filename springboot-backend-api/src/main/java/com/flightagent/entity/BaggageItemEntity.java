package com.flightagent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "baggage_items")
@Getter
@Setter
@NoArgsConstructor
public class BaggageItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private String bookingId;

    private String baggageType;
    private int quantity;

    @Column(precision = 8, scale = 2)
    private BigDecimal fee;

    public BaggageItemEntity(String bookingId, String baggageType, int quantity, BigDecimal fee) {
        this.bookingId = bookingId;
        this.baggageType = baggageType;
        this.quantity = quantity;
        this.fee = fee;
    }
}
