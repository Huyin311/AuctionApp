package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service cho các truy vấn Auction / Bids.
 */
public interface AuctionService {
    Page<Auction> listAuctions(String status, String q, Pageable pageable);
    Auction getAuction(UUID auctionId);
    Page<Bid> listBidsForAuction(UUID auctionId, Pageable pageable);
}