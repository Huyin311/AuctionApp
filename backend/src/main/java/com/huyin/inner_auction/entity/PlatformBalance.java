package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformBalance {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_commission", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (lastUpdated == null) lastUpdated = OffsetDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (totalCommission == null) totalCommission = BigDecimal.ZERO;
    }
}