package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Buyer xác nhận đã nhận hàng
 */
@Entity
@Table(name = "delivery_confirmations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryConfirmation {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "confirmed_at", nullable = false)
    private OffsetDateTime confirmedAt;

    @Column(columnDefinition = "text")
    private String note;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (confirmedAt == null) confirmedAt = OffsetDateTime.now();
    }
}