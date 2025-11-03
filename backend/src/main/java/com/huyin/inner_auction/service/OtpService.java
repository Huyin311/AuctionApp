package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Otp;
import com.huyin.inner_auction.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * OTP generation & verification service.
 */
@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${app.otp.code-digits:6}")
    private int otpDigits;

    @Value("${app.otp.cooldown-seconds:60}")
    private int otpCooldownSeconds;

    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void generateAndSendOtp(String email) {
        String normalized = email.trim().toLowerCase();

        // cooldown: don't send new OTP if last one created less than cooldown seconds ago
        Optional<Otp> lastOpt = otpRepository.findByEmailOrderByCreatedAtDesc(normalized).stream().findFirst();
        if (lastOpt.isPresent()) {
            OffsetDateTime lastCreated = lastOpt.get().getCreatedAt();
            if (lastCreated.plusSeconds(otpCooldownSeconds).isAfter(OffsetDateTime.now())) {
                // ignore or throw depending on desired behavior; here throw to let client know cooldown
                throw new RuntimeException("Please wait before requesting another code");
            }
        }

        String code = generateNumericCode(otpDigits);
        OffsetDateTime now = OffsetDateTime.now();
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(normalized)
                .code(code)
                .expiresAt(now.plusMinutes(otpExpirationMinutes))
                .verified(false)
                .attempts(0)
                .createdAt(now)
                .build();
        otpRepository.save(otp);

        String subject = "Your verification code";
        String text = "Your verification code is: " + code + "\nThis code expires in " + otpExpirationMinutes + " minutes.";
        emailService.sendSimpleMail(email, subject, text);
    }

    @Transactional
    public boolean verifyOtp(String email, String code) {
        String normalized = email.trim().toLowerCase();
        var opt = otpRepository.findFirstByEmailAndCodeAndVerifiedFalseOrderByCreatedAtDesc(normalized, code);
        if (opt.isEmpty()) return false;
        Otp otp = opt.get();
        if (otp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return false;
        }
        otp.setVerified(true);
        otpRepository.save(otp);
        return true;
    }

    /**
     * Check if there exists a verified OTP for email (most recent verified and not expired more than windowMinutes ago).
     * Use this to enforce verification during register.
     */
    public boolean hasRecentVerifiedOtp(String email, int withinMinutes) {
        String normalized = email.trim().toLowerCase();
        var opt = otpRepository.findFirstByEmailAndVerifiedTrueOrderByCreatedAtDesc(normalized);
        if (opt.isEmpty()) return false;
        Otp otp = opt.get();
        // require verified OTP created within withinMinutes
        return otp.getCreatedAt().plusMinutes(withinMinutes).isAfter(OffsetDateTime.now());
    }

    private String generateNumericCode(int digits) {
        int min = (int) Math.pow(10, digits - 1);
        int max = (int) Math.pow(10, digits) - 1;
        int num = random.nextInt(max - min + 1) + min;
        return Integer.toString(num);
    }
}