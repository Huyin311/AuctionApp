package com.huyin.inner_auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AuctionDto {
    @NotBlank
    private String title;
    private String description;
    @NotNull @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal startingPrice;
    @NotNull @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal minIncrement;
    @NotNull
    private OffsetDateTime startAt;
    @NotNull
    private OffsetDateTime endAt;
    private BigDecimal reservePrice;
}