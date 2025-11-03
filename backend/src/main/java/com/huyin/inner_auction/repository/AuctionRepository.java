package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findByStatus(String status);
}