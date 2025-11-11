package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.dto.AuctionDto;
import com.huyin.inner_auction.dto.AuctionSummaryDto;
import com.huyin.inner_auction.dto.NextBidDto;
import com.huyin.inner_auction.dto.CreateAuctionRequest;
import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.AuctionImage;
import com.huyin.inner_auction.entity.Bid;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.projection.AuctionWithImageView;
import com.huyin.inner_auction.repository.AuctionImageRepository;
import com.huyin.inner_auction.repository.AuctionRepository;
import com.huyin.inner_auction.repository.BidRepository;
import com.huyin.inner_auction.repository.UserRepository;
import com.huyin.inner_auction.service.AuctionService;
import com.huyin.inner_auction.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation đơn giản cho AuctionService.
 * Trả về entity trực tiếp (bạn có thể chuyển sang DTO nếu cần).
 */
@Service
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final BidRepository bidRepository;
    private final WalletService walletService; // optional, used for balance checks
    private final UserRepository userRepository;

    @Autowired
    public AuctionServiceImpl(AuctionRepository auctionRepository,
                              AuctionImageRepository auctionImageRepository,
                              BidRepository bidRepository,
                              WalletService walletService,
                              UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.auctionImageRepository = auctionImageRepository;
        this.bidRepository = bidRepository;
        this.walletService = walletService;
        this.userRepository = userRepository;
    }


    @Override
    public Page<AuctionDto> listAuctions(String status, String q, Pageable pageable) {
        // Ignore status / q parameters and return all auctions via repository.findAll(pageable)
        Page<Auction> page = auctionRepository.findAll(pageable);

        // batch load images for auctions in the page
        List<UUID> auctionIds = page.getContent().stream()
                .map(Auction::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<UUID, List<String>> imagesByAuction = new HashMap<>();
        if (!auctionIds.isEmpty()) {
            List<AuctionImage> imgs = auctionImageRepository.findByAuctionIdsOrderByAuctionAndOrderIndex(auctionIds);
            for (AuctionImage ai : imgs) {
                UUID aid = ai.getAuction().getId();
                imagesByAuction.computeIfAbsent(aid, k -> new ArrayList<>()).add(ai.getUrl());
            }
        }

        Page<AuctionDto> dtoPage = page.map(entity -> {
            AuctionDto dto = AuctionDto.builder().build();
            dto.setId(entity.getId());
            dto.setSellerId(entity.getSellerId());
            dto.setTitle(entity.getTitle());
            dto.setDescription(entity.getDescription());
            dto.setStartingPrice(entity.getStartingPrice());
            dto.setCurrentPrice(entity.getCurrentPrice());
            dto.setMinIncrement(entity.getMinIncrement());
            dto.setReservePrice(entity.getReservePrice());
            dto.setStatus(entity.getStatus());
            if (entity.getStartAt() != null) dto.setStartAt(OffsetDateTime.ofInstant(entity.getStartAt(), ZoneOffset.UTC));
            if (entity.getEndAt() != null) dto.setEndAt(OffsetDateTime.ofInstant(entity.getEndAt(), ZoneOffset.UTC));
            if (entity.getCreatedAt() != null) dto.setCreatedAt(OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));

            List<String> imgs = imagesByAuction.get(entity.getId());
            if (imgs != null && !imgs.isEmpty()) {
                dto.setImages(imgs);
                // set legacy imageUrl in DTO from first image so frontend code that expects imageUrl still works
                dto.setImageUrl(imgs.get(0));
            } else {
                dto.setImages(Collections.emptyList());
            }

            return dto;
        });

        return dtoPage;
    }

    @Override
    public AuctionDto getAuctionById(UUID id) {
        Optional<Auction> opt = auctionRepository.findById(id);
        if (opt.isEmpty()) return null;
        Auction a = opt.get();

        AuctionDto dto = AuctionDto.builder().build();
        dto.setId(a.getId());

        // entity now exposes sellerId directly
        dto.setSellerId(a.getSellerId());

        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setStartingPrice(a.getStartingPrice());
        dto.setCurrentPrice(a.getCurrentPrice());
        dto.setMinIncrement(a.getMinIncrement());
        dto.setReservePrice(a.getReservePrice());
        dto.setStatus(a.getStatus());

        // convert Instant -> OffsetDateTime (if Instant present)
        dto.setStartAt(a.getStartAt() == null ? null : OffsetDateTime.ofInstant(a.getStartAt(), ZoneOffset.UTC));
        dto.setEndAt(a.getEndAt() == null ? null : OffsetDateTime.ofInstant(a.getEndAt(), ZoneOffset.UTC));
        dto.setCreatedAt(a.getCreatedAt() == null ? null : OffsetDateTime.ofInstant(a.getCreatedAt(), ZoneOffset.UTC));

        // Load images (uses AuctionImage entity and repo)
        List<AuctionImage> imgs = auctionImageRepository.findByAuctionIdOrderByOrderIndexAsc(a.getId());
        if (imgs != null && !imgs.isEmpty()) {
            dto.setImageUrl(imgs.get(0).getUrl());
            dto.setImages(imgs.stream().map(AuctionImage::getUrl).collect(Collectors.toList()));
        } else {
            dto.setImages(java.util.Collections.emptyList());
        }

        return dto;
    }


    @Override
    public AuctionSummaryDto getAuctionSummary(UUID id) {
        Optional<Auction> opt = auctionRepository.findById(id);
        if (opt.isEmpty()) return null;
        Auction a = opt.get();
        return AuctionSummaryDto.builder()
                .id(a.getId())
                .currentPrice(a.getCurrentPrice())
                .minIncrement(a.getMinIncrement())
                .endAt(a.getEndAt() == null ? null : OffsetDateTime.ofInstant(a.getEndAt(), ZoneOffset.UTC))
                .status(a.getStatus())
                .build();
    }


    @Override
    public NextBidDto getNextBid(UUID id, UUID userId) {
        Optional<Auction> opt = auctionRepository.findById(id);
        if (opt.isEmpty()) {
            return NextBidDto.builder().canBid(false).reason("auction_not_found").build();
        }
        Auction a = opt.get();
        BigDecimal base = a.getCurrentPrice() != null ? a.getCurrentPrice() : a.getStartingPrice();
        BigDecimal nextMin = (base == null ? BigDecimal.ZERO : base).add(a.getMinIncrement() == null ? BigDecimal.valueOf(1) : a.getMinIncrement());
        boolean canBid = true;
        String reason = null;
        BigDecimal userBalance = null;

        // check auction status
        if (!"PUBLISHED".equalsIgnoreCase(a.getStatus()) && !"LIVE".equalsIgnoreCase(a.getStatus())) {
            canBid = false;
            reason = "auction_closed";
        }

        // if userId provided, attempt to read wallet balance (WalletService method assumed)
        if (userId != null && canBid) {
            try {
                var wallet = walletService.getWalletByUserId(userId); // adapt to actual method name in your WalletService
                if (wallet != null) {
                    userBalance = wallet.getBalance();
                    if (userBalance.compareTo(nextMin) < 0) {
                        canBid = false;
                        reason = "insufficient_funds";
                    }
                }
            } catch (Exception ex) {
                // If wallet method not available or throws, ignore and return no balance
            }
        }

        return NextBidDto.builder()
                .nextMinAmount(nextMin)
                .canBid(canBid)
                .reason(reason)
                .userBalance(userBalance)
                .build();
    }

    @Override
    public Auction getAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId).orElse(null);
    }

    @Override
    public Page<Bid> listBidsForAuction(UUID auctionId, Pageable pageable) {
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId, pageable);
    }

    /**
     * Create an auction and persist auction images.
     * Validations:
     *  - seller user must exist
     *  - startAt < endAt
     *  - startingPrice and minIncrement non-negative
     *
     * This method is transactional.
     */
    @Override
    @Transactional
    public Auction createAuction(String userId, CreateAuctionRequest request) {
        // Validate user exists
        UUID sellerUuid;
        try {
            sellerUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("invalid_user_id");
        }
        User seller = userRepository.findById(sellerUuid)
                .orElseThrow(() -> new SecurityException("user_not_found"));

        // Parse and validate times
        OffsetDateTime startAtO = (request.getStartAt() == null || request.getStartAt().isBlank())
                ? OffsetDateTime.now()
                : OffsetDateTime.parse(request.getStartAt());
        OffsetDateTime endAtO = OffsetDateTime.parse(request.getEndAt());
        if (!startAtO.isBefore(endAtO)) {
            throw new IllegalArgumentException("start_at_must_be_before_end_at");
        }

        // Validate price constraints (use BigDecimal)
        if (request.getStartingPrice() == null || request.getStartingPrice() < 0) {
            throw new IllegalArgumentException("invalid_starting_price");
        }
        if (request.getMinIncrement() == null || request.getMinIncrement() < 0) {
            throw new IllegalArgumentException("invalid_min_increment");
        }

        BigDecimal startingPrice = BigDecimal.valueOf(request.getStartingPrice());
        BigDecimal minIncrement = BigDecimal.valueOf(request.getMinIncrement());
        BigDecimal reservePrice = request.getReservePrice() != null ? BigDecimal.valueOf(request.getReservePrice()) : null;

        // Build Auction entity
        Auction a = Auction.builder()
                .id(UUID.randomUUID())
                .sellerId(sellerUuid)
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(startingPrice)
                .currentPrice(startingPrice)
                .minIncrement(minIncrement)
                .reservePrice(reservePrice)
                .startAt(startAtO.toInstant())
                .endAt(endAtO.toInstant())
                .status("PUBLISHED")
                .createdAt(Instant.now())
                .build();

        // prepare images and attach to auction (cascade persist will save them)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<AuctionImage> imgs = new ArrayList<>();
            int idx = 0;
            for (String url : request.getImages()) {
                if (url == null || url.isBlank()) continue;
                AuctionImage ai = AuctionImage.builder()
                        .id(UUID.randomUUID())
                        .auction(a) // link to parent
                        .url(url.trim())
                        .orderIndex(idx++)
                        .createdAt(Instant.now())
                        .build();
                imgs.add(ai);
            }
            // set images on auction (cascade = ALL will persist them)
            if (!imgs.isEmpty()) {
                a.setImages(imgs);
            }
        }

        // Persist auction (will cascade images)
        Auction saved = auctionRepository.save(a);

        return saved;
    }
}