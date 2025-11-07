package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ledger for escrow movements — dùng để kiểm toán
 */
@Entity
@Table(name = "escrow_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowEntry {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String type; // HOLD | ESCROW_IN | ESCROW_OUT | REFUND | RELEASE

    @Column(name = "related_entity")
    private String relatedEntity;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}