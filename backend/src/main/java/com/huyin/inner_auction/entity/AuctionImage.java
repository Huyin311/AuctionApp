package com.huyin.inner_auction.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auction_images", indexes = {
        @Index(name = "idx_auction_images_auction_id", columnList = "auction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionImage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    // Many images -> one auction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auction_image_auction"))
    @JsonBackReference // prevent infinite recursion when serializing
    private Auction auction;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    // order index for gallery ordering
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}