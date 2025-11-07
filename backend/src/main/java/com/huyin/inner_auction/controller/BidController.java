package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Bid;
import com.huyin.inner_auction.service.BidService;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @Data
    static class PlaceBidRequest {
        @DecimalMin(value = "0.01", message = "amount must be >= 0.01")
        private BigDecimal amount;
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<?> placeBid(Authentication authentication,
                                      @PathVariable String auctionId,
                                      @RequestBody PlaceBidRequest req) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        UUID aId = UUID.fromString(auctionId);
        try {
            Bid created = bidService.placeBid(userId, aId, req.getAmount());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "detail", ex.getMessage()));
        }
    }
}