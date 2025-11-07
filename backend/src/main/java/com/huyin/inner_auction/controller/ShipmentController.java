package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Shipment;
import com.huyin.inner_auction.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Seller báo đã gửi hàng (thêm tracking)
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;

    @PostMapping("/{saleId}/shipment")
    public ResponseEntity<?> createShipment(Authentication authentication, @PathVariable String saleId, @RequestBody Map<String, String> body) {
        // authentication principal -> userId
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        UUID sId = UUID.fromString(saleId);

        // TODO: kiểm tra sale.sellerId == userId (bảo mật)
        String carrier = body.get("carrier");
        String trackingNumber = body.get("trackingNumber");
        String labelUrl = body.get("labelUrl");
        Shipment sh = Shipment.builder()
                .saleId(sId)
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .labelUrl(labelUrl)
                .shippedAt(OffsetDateTime.now())
                .build();
        shipmentRepository.save(sh);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}