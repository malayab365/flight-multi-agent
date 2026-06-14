package com.flightagent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_watches")
@Getter
@Setter
@NoArgsConstructor
public class PriceWatchEntity {

    @Id
    @Column(name = "watch_id", length = 20)
    private String watchId;

    private String origin;
    private String destination;
    private String departureDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal targetPrice;

    private String email;

    @Enumerated(EnumType.STRING)
    private WatchStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;
}
