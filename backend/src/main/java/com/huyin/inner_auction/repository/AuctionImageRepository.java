package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionImageRepository extends JpaRepository<AuctionImage, UUID> {
    List<AuctionImage> findByAuctionIdOrderByOrderIndexAsc(UUID auctionId);

    @Query("select ai from AuctionImage ai " +
            "where ai.auction.id in :auctionIds " +
            "order by ai.auction.id asc")
    List<AuctionImage> findByAuctionIdsOrderByAuctionAndOrdinal(@Param("auctionIds") List<UUID> auctionIds);

    @Query("select ai from AuctionImage ai " +
            "where ai.auction.id in :auctionIds " +
            "order by ai.auction.id asc, ai.orderIndex asc")
    List<AuctionImage> findByAuctionIdsOrderByAuctionAndOrderIndex(@Param("auctionIds") List<UUID> auctionIds);

}