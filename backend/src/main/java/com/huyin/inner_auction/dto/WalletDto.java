package com.huyin.inner_auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simple wallet DTO returned to frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private UUID userId;
    private BigDecimal balance;     // total balance
    private BigDecimal heldAmount;  // funds currently held (holds)
    private BigDecimal available;   // balance - heldAmount
}