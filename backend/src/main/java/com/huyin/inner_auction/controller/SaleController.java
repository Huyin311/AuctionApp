package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints cho buyer xác nhận và seller gửi shipment
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    // Buyer xác nhận đã nhận hàng -> release tiền
    @PostMapping("/{saleId}/confirm-delivery")
    public ResponseEntity<?> confirmDelivery(Authentication authentication, @PathVariable String saleId) {
        UUID buyerId = UUID.fromString(authentication.getPrincipal().toString());
        UUID sId = UUID.fromString(saleId);
        try {
            saleService.confirmDelivery(buyerId, sId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "detail", ex.getMessage()));
        }
    }
}