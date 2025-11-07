package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Shipment info provided by seller after sale
 */
@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 200)
    private String trackingNumber;

    @Column(name = "label_url")
    private String labelUrl;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "estimated_delivery_at")
    private OffsetDateTime estimatedDeliveryAt;

    @Column(name = "proof_of_shipment", columnDefinition = "jsonb")
    private String proofOfShipment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}