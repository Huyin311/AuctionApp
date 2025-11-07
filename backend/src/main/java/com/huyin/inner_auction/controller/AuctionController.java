package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.Bid;
import com.huyin.inner_auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Các API đọc cho Auctions:
 * - GET /api/auctions?page=&size=&status=&q= : danh sách có phân trang / lọc
 * - GET /api/auctions/{id} : chi tiết auction
 * - GET /api/auctions/{id}/bids?page=&size= : lịch sử bids (theo thời gian)
 *
 * Vietnamese: controller cung cấp API public để frontend hiển thị danh sách và chi tiết auction.
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public ResponseEntity<?> listAuctions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Auction> result = auctionService.listAuctions(status, q, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuction(@PathVariable String id) {
        try {
            UUID auctionId = UUID.fromString(id);
            Auction a = auctionService.getAuction(auctionId);
            if (a == null) return ResponseEntity.status(404).body(Map.of("error", "auction_not_found"));
            return ResponseEntity.ok(a);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_uuid"));
        }
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<?> getAuctionBids(
            @PathVariable String id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        try {
            UUID auctionId = UUID.fromString(id);
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
            Page<Bid> bids = auctionService.listBidsForAuction(auctionId, pageable);
            return ResponseEntity.ok(bids);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_uuid"));
        }
    }
}