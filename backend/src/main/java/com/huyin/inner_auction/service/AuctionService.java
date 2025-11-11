package com.huyin.inner_auction.service;

import com.huyin.inner_auction.dto.AuctionDto;
import com.huyin.inner_auction.dto.AuctionSummaryDto;
import com.huyin.inner_auction.dto.CreateAuctionRequest;
import com.huyin.inner_auction.dto.NextBidDto;
import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service cho các truy vấn Auction / Bids.
 */
public interface AuctionService {
    Page<AuctionDto> listAuctions(String status, String qLike, Pageable pageable);
    Auction getAuction(UUID auctionId);
    Page<Bid> listBidsForAuction(UUID auctionId, Pageable pageable);
    AuctionDto getAuctionById(UUID id);
    AuctionSummaryDto getAuctionSummary(UUID id);
    NextBidDto getNextBid(UUID id, UUID userId);
    Auction createAuction(String userId, CreateAuctionRequest request) throws IllegalArgumentException, SecurityException;
}