package com.huyin.inner_auction.service;

/**
 * Contract for OTP generation and verification.
 */
public interface OtpService {

    /**
     * Generate an OTP for the given email and send it (e.g. by email/SMS).
     */
    void sendOtp(String email);

    /**
     * Alias / compatibility method (optional).
     * Implementations can delegate to sendOtp.
     */
    default void generateAndSendOtp(String email) {
        sendOtp(email);
    }

    /**
     * Verify the provided OTP code for the given email.
     *
     * @return true if verification succeeded, false otherwise
     */
    boolean verifyOtp(String email, String code);

    /**
     * Check whether there exists a recently verified OTP for the email within the given window (minutes).
     */
    boolean hasRecentVerifiedOtp(String email, int withinMinutes);
}