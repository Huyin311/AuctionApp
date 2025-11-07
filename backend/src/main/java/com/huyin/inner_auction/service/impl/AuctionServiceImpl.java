package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.Bid;
import com.huyin.inner_auction.repository.AuctionRepository;
import com.huyin.inner_auction.repository.BidRepository;
import com.huyin.inner_auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation đơn giản cho AuctionService.
 * Trả về entity trực tiếp (bạn có thể chuyển sang DTO nếu cần).
 */
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Override
    public Page<Auction> listAuctions(String status, String q, Pageable pageable) {
        // Very simple filtering:
        if (status != null && !status.isBlank() && q != null && !q.isBlank()) {
            return auctionRepository.findByStatusAndTitleContainingIgnoreCase(status, q, pageable);
        } else if (status != null && !status.isBlank()) {
            return auctionRepository.findByStatus(status, pageable);
        } else if (q != null && !q.isBlank()) {
            return auctionRepository.findByTitleContainingIgnoreCase(q, pageable);
        } else {
            return auctionRepository.findAll(pageable);
        }
    }

    @Override
    public Auction getAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId).orElse(null);
    }

    @Override
    public Page<Bid> listBidsForAuction(UUID auctionId, Pageable pageable) {
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId, pageable);
    }
}