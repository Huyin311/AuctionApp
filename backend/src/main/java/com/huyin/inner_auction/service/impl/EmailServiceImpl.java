package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Default EmailService implementation. Attempts to send via JavaMailSender;
 * if sending fails (e.g. no SMTP configured in dev) it logs the message instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String code) {
        String subject = "Your verification code";
        String text = "Your verification code is: " + code + "\n"
                + "If you did not request this code, please ignore this email.";
        sendSimpleMail(to, subject, text);
    }

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Sent email to {}", to);
        } catch (Exception ex) {
            // In development environments without SMTP configured, we log the email content.
            log.warn("Failed to send mail (check SMTP configuration). Email content for {}: {}", to, text);
        }
    }
}