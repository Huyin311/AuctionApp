package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.UserRepository;
import com.huyin.inner_auction.security.JwtUtil;
import com.huyin.inner_auction.service.AuthService;
import com.huyin.inner_auction.service.OtpService;
import com.huyin.inner_auction.util.PasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Concrete implementation of AuthService with OTP enforcement.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthServiceImpl(UserRepository userRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder,
                           OtpService otpService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Override
    @Transactional
    public String register(String email, String rawPassword, String role) {
        if (email == null || rawPassword == null) throw new IllegalArgumentException("email/password required");
        String normalized = email.trim().toLowerCase();

        if (!PasswordValidator.isStrong(rawPassword)) {
            throw new RuntimeException("Password not strong: " + PasswordValidator.explain(rawPassword));
        }

        // require recent verified OTP (within e.g. 10 minutes)
        boolean verified = otpService.hasRecentVerifiedOtp(normalized, 10);
        if (!verified) {
            throw new RuntimeException("Email not verified by OTP");
        }

        userRepository.findByEmail(normalized).ifPresent(u -> {
            throw new RuntimeException("Email already registered");
        });

        User u = User.builder()
                .id(UUID.randomUUID())
                .email(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role == null ? "BUYER" : role.toUpperCase())
                .depositPaid(false)
                .balance(BigDecimal.valueOf(0.00))
                .createdAt(OffsetDateTime.now())
                .build();

        userRepository.save(u);
        return jwtUtil.generateToken(u.getId().toString());
    }

    @Override
    public String login(String email, String rawPassword) {
        if (email == null || rawPassword == null) throw new IllegalArgumentException("email/password required");
        String normalized = email.trim().toLowerCase();
        User u = userRepository.findByEmail(normalized).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        return jwtUtil.generateToken(u.getId().toString());
    }

    @Override
    public UUID findUserIdByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).map(User::getId).orElse(null);
    }
}