package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionFinalizerScheduler {

    private final BidService bidService;

    // Run every minute
    @Scheduled(fixedDelayString = "${app.auction.finalizer-interval-ms:60000}")
    public void runFinalizer() {
        try {
            bidService.finalizeEndedAuctions();
        } catch (Exception ex) {
            log.error("Error running auction finalizer", ex);
        }
    }
}