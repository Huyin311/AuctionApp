package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.security.JwtUtil;
import com.huyin.inner_auction.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Wallet endpoints (protected).
 * Uses authenticated principal (userId string) from JwtAuthFilter.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    public WalletController(WalletService walletService, JwtUtil jwtUtil) {
        this.walletService = walletService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(Authentication authentication, @RequestBody Map<String, Object> body) {
        if (authentication == null) throw new RuntimeException("Unauthorized");
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        double amount = Double.parseDouble(body.get("amount").toString());
        walletService.topUp(userId, amount);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) throw new RuntimeException("Unauthorized");
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        User u = walletService.getProfile(userId);
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "balance", u.getBalance(),
                "depositPaid", u.getDepositPaid()
        ));
    }
}