package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dispute opened by buyer/seller to block release
 */
@Entity
@Table(name = "disputes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "opener_id", nullable = false)
    private UUID openerId;

    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "text")
    private String details;

    @Column(length = 30, nullable = false)
    private String status = "OPEN";

    @Column(columnDefinition = "text")
    private String resolution;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}