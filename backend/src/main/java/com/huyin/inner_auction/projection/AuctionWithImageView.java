package com.huyin.inner_auction.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection interface for native query that returns auction fields + first image url
 * Use Instant for timestamptz columns to avoid projection conversion errors.
 */
public interface AuctionWithImageView {
    UUID getId();
    UUID getSellerId();
    String getTitle();
    String getDescription();
    BigDecimal getStartingPrice();
    BigDecimal getCurrentPrice();
    BigDecimal getMinIncrement();
    Instant getStartAt();   // changed to Instant
    Instant getEndAt();     // changed to Instant
    BigDecimal getReservePrice();
    String getStatus();
    Instant getCreatedAt(); // changed to Instant
    String getImageUrl(); // may be null if no image
}