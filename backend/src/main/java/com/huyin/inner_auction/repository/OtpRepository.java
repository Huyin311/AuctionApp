package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    List<Otp> findByEmailOrderByCreatedAtDesc(String email);

    Optional<Otp> findFirstByEmailAndCodeAndVerifiedFalseOrderByCreatedAtDesc(String email, String code);

    Optional<Otp> findFirstByEmailAndVerifiedTrueOrderByCreatedAtDesc(String email);
}