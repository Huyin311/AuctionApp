package com.huyin.inner_auction.dto;

import java.time.OffsetDateTime;

/**
 * DTO used for create/update requests for Auction.
 * Fields use camelCase to match JSON from frontend.
 */
public class AuctionDto {
    public String title;
    public String description;
    public Double startingPrice;
    public Double minIncrement;
    public OffsetDateTime startAt;
    public OffsetDateTime endAt;
    public Double reservePrice;
}