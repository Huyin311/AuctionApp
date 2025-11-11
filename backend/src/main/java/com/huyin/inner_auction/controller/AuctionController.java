package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.dto.AuctionDto;
import com.huyin.inner_auction.dto.CreateAuctionRequest;
import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.Bid;
import com.huyin.inner_auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
//import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

    @PostMapping
    public ResponseEntity<?> createAuction(@Validated @RequestBody CreateAuctionRequest req, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        String userId = auth.getName(); // adapt if you store id in claim differently

        try {
            Auction created = auctionService.createAuction(userId, req);
            return ResponseEntity.created(URI.create("/api/auctions/" + created.getId())).body(created);
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().body(Map.of("error", ia.getMessage()));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", se.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "server_error"));
        }
    }

    @GetMapping
    public ResponseEntity<?> listAuctions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        String qParam = (q == null) ? null : q.trim();
        if (qParam != null && qParam.isEmpty()) qParam = null;

        // prepare LIKE pattern in Java (or null)
        String qLike = (qParam == null) ? null : "%" + qParam + "%";

        Page<AuctionDto> result = auctionService.listAuctions(status, qLike, pageable);
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