package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldRepository extends JpaRepository<Hold, UUID> {
    List<Hold> findByUserIdAndStatus(UUID userId, String status);

    Optional<Hold> findTopByAuctionIdAndStatusOrderByCreatedAtDesc(UUID auctionId, String status);

    // find top hold for auction excluding a specific user (useful to find previous top before current user's update)
    Optional<Hold> findTopByAuctionIdAndStatusAndUserIdNotOrderByAmountDescCreatedAtDesc(UUID auctionId, String status, UUID excludeUserId);

    // top by amount then createdAt (highest bidder)
    Optional<Hold> findTopByAuctionIdAndStatusOrderByAmountDescCreatedAtDesc(UUID auctionId, String status);

    List<Hold> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);
}