package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Transaction;
import com.huyin.inner_auction.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Data
    static class TopUpRequest {
        @DecimalMin(value = "0.01", message = "amount must be >= 0.01")
        private BigDecimal amount;
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(Authentication authentication, @Valid @RequestBody TopUpRequest req) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        try {
            BigDecimal newBalance = walletService.topUp(userId, req.getAmount());
            return ResponseEntity.ok(Map.of("status", "ok", "balance", newBalance));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return ResponseEntity.ok(Map.of("id", userId.toString(), "balance", walletService.getBalance(userId)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> transactions(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        List<Transaction> txs = walletService.listTransactions(userId);
        return ResponseEntity.ok(txs);
    }
}