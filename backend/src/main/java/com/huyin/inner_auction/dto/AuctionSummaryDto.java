package com.huyin.inner_auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for polling / summary use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionSummaryDto {
    private UUID id;
    private BigDecimal currentPrice;
    private BigDecimal minIncrement;
    private OffsetDateTime endAt;
    private String status;
}