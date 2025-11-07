package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for auction operations (manual finalize).
 */
@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
public class AdminAuctionController {

    private final BidService bidService;

    @PostMapping("/{auctionId}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> finalizeAuction(@PathVariable String auctionId) {
        try {
            UUID aId = UUID.fromString(auctionId);
            bidService.finalizeAuction(aId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_uuid"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "detail", ex.getMessage()));
        }
    }
}