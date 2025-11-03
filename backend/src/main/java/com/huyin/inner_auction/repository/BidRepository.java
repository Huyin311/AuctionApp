package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    // find highest bid for auction
    @Query(value = "SELECT b.* FROM bids b WHERE b.auction_id = ?1 ORDER BY b.amount DESC, b.created_at ASC LIMIT 1", nativeQuery = true)
    Optional<Bid> findHighestBidByAuctionId(UUID auctionId);
}