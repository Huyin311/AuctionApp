package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    /**
     * Get the most recent OTP record for an email (by created_at desc).
     * Used for cooldown checks and verification attempts.
     */
    Optional<Otp> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * Find the most recent unverified OTP that matches the given code for the email.
     * Used during verification to ensure we validate the latest unverified OTP.
     */
    Optional<Otp> findFirstByEmailAndCodeAndVerifiedFalseOrderByCreatedAtDesc(String email, String code);

    /**
     * Find the most recent verified OTP for an email.
     * Used to check whether a recent verified OTP exists (e.g., before registration).
     */
    Optional<Otp> findFirstByEmailAndVerifiedTrueOrderByCreatedAtDesc(String email);
}