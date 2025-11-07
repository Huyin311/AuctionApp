package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.Otp;
import com.huyin.inner_auction.repository.OtpRepository;
import com.huyin.inner_auction.service.EmailService;
import com.huyin.inner_auction.service.OtpService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${app.otp.code-digits:6}")
    private int otpDigits;

    @Value("${app.otp.cooldown-seconds:60}")
    private int otpCooldownSeconds;

    @Override
    @Transactional
    public void sendOtp(String email) {
        String normalized = email.trim().toLowerCase();

        Optional<Otp> lastOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalized);
        if (lastOpt.isPresent()) {
            OffsetDateTime lastCreated = lastOpt.get().getCreatedAt();
            if (lastCreated.plusSeconds(otpCooldownSeconds).isAfter(OffsetDateTime.now())) {
                throw new RuntimeException("Please wait before requesting another code");
            }
        }

        String code = generateNumericCode(otpDigits);
        OffsetDateTime now = OffsetDateTime.now();
        Otp otp = Otp.builder()
                .id(null)
                .email(normalized)
                .code(code)
                .expiresAt(now.plusMinutes(otpExpirationMinutes))
                .verified(false)
                .createdAt(now)
                .build();
        otpRepository.save(otp);
        emailService.sendOtpEmail(normalized, code);
    }

    // optional explicit alias (not necessary if you used default in interface)
    @Override
    public void generateAndSendOtp(String email) {
        sendOtp(email);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String code) {
        String normalized = email.trim().toLowerCase();
        Optional<Otp> opt = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalized);
        if (opt.isEmpty()) return false;
        Otp otp = opt.get();
        if (otp.isVerified()) return false;
        if (!otp.getCode().equals(code)) return false;
        if (otp.getExpiresAt() != null && otp.getExpiresAt().isBefore(OffsetDateTime.now())) return false;
        otp.setVerified(true);
        otpRepository.save(otp);
        return true;
    }

    @Override
    public boolean hasRecentVerifiedOtp(String email, int withinMinutes) {
        String normalized = email.trim().toLowerCase();
        Optional<Otp> opt = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalized);
        if (opt.isEmpty()) return false;
        Otp otp = opt.get();
        if (!otp.isVerified()) return false;
        return otp.getCreatedAt().plusMinutes(withinMinutes).isAfter(OffsetDateTime.now());
    }

    private String generateNumericCode(int digits) {
        if (digits <= 0) digits = 6;
        int min = (int) Math.pow(10, digits - 1);
        int max = (int) Math.pow(10, digits) - 1;
        int num = ThreadLocalRandom.current().nextInt(min, max + 1);
        return Integer.toString(num);
    }
}