package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("dev") // chỉ đăng ký controller khi profile = dev
public class DevController {

    private final UserRepository userRepository;

    @PostMapping("/enable-deposit")
    public ResponseEntity<?> enableDeposit(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "email required"));
        return userRepository.findByEmail(email.trim().toLowerCase()).map(user -> {
            user.setDepositPaid(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "ok"));
        }).orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "user_not_found")));
    }
}