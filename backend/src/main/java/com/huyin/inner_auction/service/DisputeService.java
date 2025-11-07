package com.huyin.inner_auction.service;

import java.util.UUID;

/**
 * Service cho dispute xử lý bởi admin.
 */
public interface DisputeService {
    /**
     * Resolve a dispute:
     * action: "release" => release funds to seller
     *         "refund"  => refund buyer
     *         "split"   => split amount to seller and buyer (amountToSeller must be provided)
     */
    void resolveDispute(UUID adminUserId, UUID disputeId, String action, java.math.BigDecimal amountToSeller, String note);
}