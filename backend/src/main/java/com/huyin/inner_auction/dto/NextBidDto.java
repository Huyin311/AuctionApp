package com.huyin.inner_auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Next bid info: next minimum amount and optional reason / flags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextBidDto {
    private BigDecimal nextMinAmount;
    private boolean canBid;
    private String reason; // e.g., "auction_closed", "insufficient_funds", null if ok
    private BigDecimal userBalance; // nullable (present if authenticated and wallet available)
}