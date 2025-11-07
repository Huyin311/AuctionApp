package com.huyin.inner_auction.service;

/**
 * Email sending contract used by the application.
 * Implementations should try to send email but may fallback to logging in dev.
 */
public interface EmailService {

    /**
     * Send OTP email (high level helper used by OtpService).
     *
     * @param to   recipient email
     * @param code otp code to include in email body
     */
    void sendOtpEmail(String to, String code);

    /**
     * Send a simple plain-text email.
     *
     * @param to      recipient email
     * @param subject subject of the email
     * @param text    plain text body
     */
    void sendSimpleMail(String to, String subject, String text);
}