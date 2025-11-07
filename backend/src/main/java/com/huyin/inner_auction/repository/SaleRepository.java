package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    List<Sale> findByStatus(String status);
    List<Sale> findBySellerIdAndStatus(UUID sellerId, String status);
    List<Sale> findByBuyerIdAndStatus(UUID buyerId, String status);
}