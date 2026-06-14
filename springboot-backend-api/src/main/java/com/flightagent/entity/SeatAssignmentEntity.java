package com.flightagent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_assignments")
@Getter
@Setter
@NoArgsConstructor
public class SeatAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatType;
    private String seatNumber;

    @Column(precision = 8, scale = 2)
    private BigDecimal fee;

    public SeatAssignmentEntity(String seatType, String seatNumber, BigDecimal fee) {
        this.seatType = seatType;
        this.seatNumber = seatNumber;
        this.fee = fee;
    }
}
