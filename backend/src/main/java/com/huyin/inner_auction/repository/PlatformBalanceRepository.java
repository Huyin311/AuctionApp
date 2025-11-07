package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.PlatformBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformBalanceRepository extends JpaRepository<PlatformBalance, UUID> {
    // Typically there will be a single row — callers can use findAll().stream().findFirst()
}