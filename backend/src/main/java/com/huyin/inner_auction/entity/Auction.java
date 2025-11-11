package com.huyin.inner_auction.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "seller_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID sellerId;

    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "starting_price")
    private BigDecimal startingPrice;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "min_increment")
    private BigDecimal minIncrement;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "reserve_price")
    private BigDecimal reservePrice;

    private String status;

    @Column(name = "winner_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID winnerId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    private Boolean settled;

    // NOTE: removed legacy image_url field here — images live in auction_images table now.

    // Relationship: one Auction -> many AuctionImage
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @JsonManagedReference // serialize images; paired with @JsonBackReference on AuctionImage.auction
    @Builder.Default
    private List<AuctionImage> images = new ArrayList<>();
}