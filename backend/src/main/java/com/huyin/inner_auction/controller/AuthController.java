package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.service.AuthService;
import com.huyin.inner_auction.service.OtpService;
import com.huyin.inner_auction.util.PasswordValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth controller: includes send-otp, verify-otp, register, login and token (alias).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "email required"));

        // gọi phương thức hiện có trong service
        otpService.sendOtp(email);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || code == null) return ResponseEntity.badRequest().body(Map.of("error", "email and code required"));
        boolean ok = otpService.verifyOtp(email, code);
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "invalid_or_expired"));
        return ResponseEntity.ok(Map.of("status", "verified"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // password strength checked in service too; early feedback here
        if (!PasswordValidator.isStrong(req.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "weak_password", "detail", PasswordValidator.explain(req.password())));
        }
        String token = authService.register(req.email(), req.password(), req.role());
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.email(), req.password());
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    /**
     * Alias endpoint to obtain JWT token (same behavior as /api/auth/login).
     * Clients can POST to /api/auth/token with email/password to get the token.
     */
    @PostMapping("/token")
    public ResponseEntity<?> token(@Valid @RequestBody TokenRequest req) {
        String token = authService.login(req.email(), req.password());
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    public static record RegisterRequest(@Email @NotBlank String email,
                                         @NotBlank String password,
                                         String role) {}

    public static record LoginRequest(@Email @NotBlank String email,
                                      @NotBlank String password) {}

    public static record TokenRequest(@Email @NotBlank String email,
                                      @NotBlank String password) {}
}