package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "commission_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionLogs {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "auction_id")
    private UUID auctionId;

    @Column(name = "payout_id")
    private UUID payoutId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "commission_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "text")
    private String note;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}