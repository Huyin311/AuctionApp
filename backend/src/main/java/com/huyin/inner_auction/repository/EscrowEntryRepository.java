package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.EscrowEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EscrowEntryRepository extends JpaRepository<EscrowEntry, UUID> {
}