package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Dispute;
import com.huyin.inner_auction.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoint để mở dispute
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;

    @PostMapping("/{saleId}/dispute")
    public ResponseEntity<?> openDispute(Authentication authentication, @PathVariable String saleId, @RequestBody Map<String, String> body) {
        UUID opener = UUID.fromString(authentication.getPrincipal().toString());
        UUID sId = UUID.fromString(saleId);
        String reason = body.get("reason");
        String details = body.get("details");
        Dispute d = Dispute.builder()
                .saleId(sId)
                .openerId(opener)
                .reason(reason)
                .details(details)
                .status("OPEN")
                .build();
        disputeRepository.save(d);
        return ResponseEntity.ok(Map.of("status", "ok", "disputeId", d.getId()));
    }
}