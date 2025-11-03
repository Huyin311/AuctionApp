package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type; // TOPUP | HOLD | RELEASE | PAYMENT | REFUND | FEE | PAYOUT

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_id", columnDefinition = "uuid")
    private UUID referenceId;

    private String relatedEntity;

    @Column(nullable = false)
    private String status = "COMPLETED"; // PENDING | COMPLETED | FAILED

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}