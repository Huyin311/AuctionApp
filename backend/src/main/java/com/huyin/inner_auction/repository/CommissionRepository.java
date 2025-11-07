package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.CommissionLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Alias repository kept for compatibility if code references CommissionRepository.
 * Internally this works with the commission_logs table via CommissionLogs entity.
 */
@Repository
public interface CommissionRepository extends JpaRepository<CommissionLogs, UUID> {
}