package com.huyin.inner_auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Simple email sender. If you do not want to configure SMTP for dev,
 * you can replace mailSender.send(...) with a log.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Logger log = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleMail(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Sent OTP email to {}", to);
        } catch (Exception ex) {
            // For dev you might not have SMTP configured; log instead of failing
            log.warn("Failed to send mail (check SMTP configuration). OTP content for {}: {}", to, text);
        }
    }
}