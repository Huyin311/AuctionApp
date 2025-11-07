package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.DeliveryConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryConfirmationRepository extends JpaRepository<DeliveryConfirmation, UUID> {
}