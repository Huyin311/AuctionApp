package com.huyin.inner_auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Auction returned to frontend. Uses Lombok to generate boilerplate.
 * Now includes images (ordered list of URLs).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDto {
    private UUID id;
    private UUID sellerId;
    private String title;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal minIncrement;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private BigDecimal reservePrice;
    private String status;
    private OffsetDateTime createdAt;
    private String imageUrl;      // single image (legacy)
    private List<String> images;  // ordered image URLs for gallery
}