package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dev helper endpoints for testing only (do NOT use in production).
 * - POST /api/dev/enable-deposit { "email": "seller@example.com" } -> sets deposit_paid = true
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final UserRepository userRepository;

    public DevController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/enable-deposit")
    public ResponseEntity<?> enableDeposit(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("error", "email required"));
        User u = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (u == null) return ResponseEntity.badRequest().body(Map.of("error", "user not found"));
        u.setDepositPaid(true);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}