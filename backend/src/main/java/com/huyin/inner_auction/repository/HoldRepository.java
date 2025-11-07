package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldRepository extends JpaRepository<Hold, UUID> {
    List<Hold> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);
    List<Hold> findByUserIdAndStatus(UUID userId, String status);

    /**
     * Find the most recent hold for an auction with a given status (e.g. HELD).
     */
    Optional<Hold> findTopByAuctionIdAndStatusOrderByCreatedAtDesc(UUID auctionId, String status);
}