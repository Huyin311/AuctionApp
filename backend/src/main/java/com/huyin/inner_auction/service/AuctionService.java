package com.huyin.inner_auction.service;

import com.huyin.inner_auction.dto.AuctionDto;
import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.AuctionRepository;
import com.huyin.inner_auction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Auction business logic: create/list/get.
 */
@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public AuctionService(AuctionRepository auctionRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Auction create(UUID sellerId, AuctionDto dto) {
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!"SELLER".equalsIgnoreCase(seller.getRole())) {
            throw new RuntimeException("User is not a seller");
        }
        if (!Boolean.TRUE.equals(seller.getDepositPaid())) {
            throw new RuntimeException("Seller has not paid deposit");
        }

        Auction a = Auction.builder()
                .id(UUID.randomUUID())
                .sellerId(sellerId)
                .title(dto.title)
                .description(dto.description)
                .startingPrice(BigDecimal.valueOf(dto.startingPrice))
                .currentPrice(null)
                .minIncrement(BigDecimal.valueOf(dto.minIncrement == null ? 1.0 : dto.minIncrement))
                .startAt(dto.startAt)
                .endAt(dto.endAt)
                .reservePrice(BigDecimal.valueOf(dto.reservePrice))
                .status("PUBLISHED")
                .createdAt(OffsetDateTime.now())
                .build();

        return auctionRepository.save(a);
    }

    public List<Auction> listPublished() {
        return auctionRepository.findByStatus("PUBLISHED");
    }

    public Auction get(UUID id) {
        return auctionRepository.findById(id).orElseThrow(() -> new RuntimeException("Auction not found"));
    }
}