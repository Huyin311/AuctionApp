package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "starting_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal startingPrice;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "min_increment", precision = 18, scale = 2, nullable = false)
    private BigDecimal minIncrement;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "reserve_price", precision = 18, scale = 2)
    private BigDecimal reservePrice;

    @Column(nullable = false, length = 30)
    private String status = "PUBLISHED";

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "commission_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "commission_rate", precision = 18, scale = 2, nullable = false)
    private BigDecimal commissionRate;

    @Column(name = "final_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalPrice;

    @Column(name = "settled")
    private Boolean settled;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}