package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.dto.AuctionDto;
import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Auction endpoints:
 * - POST /api/auctions (seller) create auction
 * - GET /api/auctions list
 * - GET /api/auctions/{id} detail
 */
@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<Auction> create(Authentication authentication, @RequestBody AuctionDto dto) {
        if (authentication == null) throw new RuntimeException("Unauthorized");
        UUID sellerId = UUID.fromString(authentication.getPrincipal().toString());
        Auction a = auctionService.create(sellerId, dto);
        return ResponseEntity.ok(a);
    }

    @GetMapping
    public ResponseEntity<List<Auction>> list() {
        return ResponseEntity.ok(auctionService.listPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auction> get(@PathVariable String id) {
        return ResponseEntity.ok(auctionService.get(UUID.fromString(id)));
    }
}