package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.service.DisputeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints to resolve disputes.
 */
@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
public class AdminDisputeController {

    private final DisputeService disputeService;

    @Data
    static class ResolveRequest {
        private String action; // release | refund | split
        private BigDecimal amountToSeller; // optional for split
        private String note;
    }

    @PostMapping("/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolve(@PathVariable String disputeId, @RequestBody ResolveRequest req,
                                     @RequestHeader(value = "X-Admin-User-Id", required = false) String adminHeader) {
        try {
            UUID dId = UUID.fromString(disputeId);
            UUID adminId = adminHeader == null ? null : UUID.fromString(adminHeader);
            disputeService.resolveDispute(adminId, dId, req.getAction(), req.getAmountToSeller(), req.getNote());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "detail", ex.getMessage()));
        }
    }
}