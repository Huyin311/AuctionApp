package com.huyin.inner_auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateAuctionRequest {
    @NotBlank
    @Size(max = 300)
    private String title;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double startingPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double minIncrement;

    // optional
    @DecimalMin(value = "0.0", inclusive = true)
    private Double reservePrice;

    // ISO datetime string (optional) - if null -> startAt = now()
    private String startAt;

    // ISO datetime string - recommended required (validate)
    @NotNull
    private String endAt;

    // optional list of image URLs (already uploaded or external)
    private List<@NotBlank String> images;
}