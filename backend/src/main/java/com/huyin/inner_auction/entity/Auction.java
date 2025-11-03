package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "seller_id", columnDefinition = "uuid", nullable = false)
    private UUID sellerId; // not using ManyToOne to keep entities simple for MVP

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal startingPrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minIncrement = BigDecimal.valueOf(1.00);

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(precision = 18, scale = 2)
    private BigDecimal reservePrice;

    @Column(nullable = false)
    private String status = "PUBLISHED"; // DRAFT | PUBLISHED | LIVE | ENDED | CANCELLED

    @Column(name = "winner_id", columnDefinition = "uuid")
    private UUID winnerId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}