package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldRepository extends JpaRepository<Hold, UUID> {
    List<Hold> findByAuctionIdAndStatus(UUID auctionId, String status);
    Optional<Hold> findByUserIdAndAuctionIdAndStatus(UUID userId, UUID auctionId, String status);
}