package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Bid;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service handling bids and auction finalization.
 */
public interface BidService {
    /**
     * Place a bid for a given auction by a user.
     *
     * @param userId    bidder user id
     * @param auctionId auction id
     * @param amount    bid amount
     * @return created Bid
     */
    Bid placeBid(UUID userId, UUID auctionId, BigDecimal amount);

    /**
     * Finalize auctions that have ended (settle payments, payouts).
     * Can be invoked by scheduler or manually.
     */
    void finalizeEndedAuctions();

    /**
     * Finalize a single auction (admin/manual).
     */
    void finalizeAuction(UUID auctionId);
}