package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.CommissionLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionLogsRepository extends JpaRepository<CommissionLogs, UUID> {
    List<CommissionLogs> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);
    List<CommissionLogs> findByAuctionId(UUID auctionId);
}