package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.*;
import com.huyin.inner_auction.repository.*;
import com.huyin.inner_auction.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý đặt giá và finalize auctions.
 *
 * - placeBid: giờ chỉ tạo/ghi Holds (không trừ balance). Khi auction finalize thì mới trừ winner balance.
 * - finalizeEndedAuctions / finalizeAuction: xử lý khi auction kết thúc, trừ tiền người thắng, tạo sale/payout/commission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidServiceImpl implements BidService {

    private final EntityManager em;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final HoldRepository holdRepository;
    private final TransactionRepository transactionRepository;
    private final EscrowEntryRepository escrowEntryRepository;
    private final SaleRepository saleRepository;
    private final PayoutRepository payoutRepository;
    private final CommissionLogsRepository commissionLogsRepository;
    private final PlatformBalanceRepository platformBalanceRepository;

    // Helper: persist hold and force flush + refresh so we can reliably inspect DB state in same tx.
    private Hold persistAndFlushHold(Hold h) {
        Hold saved = holdRepository.save(h);
        try {
            em.flush();
            try { em.refresh(saved); } catch (Throwable ignored) {}
        } catch (Exception e) {
            log.error("persistAndFlushHold failed for hold {}: {}", h.getId(), e.getMessage(), e);
            throw e;
        }
        return saved;
    }

    // Helper: flush entity manager and log SQL execution (useful for debug)
    private void flushEm(String marker) {
        try {
            em.flush();
            log.debug("em.flush() completed [{}]", marker);
        } catch (Exception e) {
            log.error("em.flush() failed [{}]: {}", marker, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Place a bid.
     * IMPORTANT CHANGE: do NOT modify users.balance here. Only create/merge holds.
     */
    @Override
    @Transactional
    public Bid placeBid(UUID userId, UUID auctionId, BigDecimal amount) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        if (auctionId == null) throw new IllegalArgumentException("auctionId required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        // Lock auction row to serialize updates to this auction
        Auction auction = em.find(Auction.class, auctionId, LockModeType.PESSIMISTIC_WRITE);
        if (auction == null) throw new IllegalStateException("auction not found");

        Instant nowInstant = Instant.now();

        if (!"PUBLISHED".equalsIgnoreCase(auction.getStatus())) {
            throw new IllegalStateException("auction not open for bidding");
        }
        // null-safe checks for start/end (entity uses Instant)
        Instant startAt = auction.getStartAt();
        Instant endAt = auction.getEndAt();
        if ((startAt != null && startAt.isAfter(nowInstant)) || (endAt != null && endAt.isBefore(nowInstant))) {
            throw new IllegalStateException("auction not active");
        }

        BigDecimal current = auction.getCurrentPrice() == null ? auction.getStartingPrice() : auction.getCurrentPrice();
        BigDecimal requiredMin = (current == null ? BigDecimal.ZERO : current)
                .add(auction.getMinIncrement() == null ? BigDecimal.ONE : auction.getMinIncrement());
        if (amount.compareTo(requiredMin) < 0) {
            throw new IllegalArgumentException("bid too low; minimum is " + requiredMin);
        }

        // Lock user row (we may check balance for available, but won't change it now)
        User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new IllegalStateException("user not found");

        // Retrieve HELD holds for this user (across auctions)
        List<Hold> heldByUserAll = holdRepository.findByUserIdAndStatus(userId, "HELD");

        // Compute how much user already has held for THIS auction
        BigDecimal userHeldForAuction = heldByUserAll.stream()
                .filter(h -> auctionId.equals(h.getAuctionId()))
                .map(h -> h.getAmount() == null ? BigDecimal.ZERO : h.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total held across auctions
        BigDecimal totalHeld = heldByUserAll.stream()
                .map(h -> h.getAmount() == null ? BigDecimal.ZERO : h.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();

        // Now available is balance - (totalHeld - userHeldForAuction)
        // Explanation: balance is NOT reduced by holds in this model, so we subtract other holds.
        BigDecimal available = balance.subtract(totalHeld.subtract(userHeldForAuction));

        // Additional amount needed on top of user's existing hold for this auction
        BigDecimal additional = amount.subtract(userHeldForAuction);
        if (additional.compareTo(BigDecimal.ZERO) < 0) additional = BigDecimal.ZERO;

        log.debug("placeBid start: user={} auction={} amount={} balance={} userHeldForAuction={} totalHeld={} available={} additional={}",
                userId, auctionId, amount, balance, userHeldForAuction, totalHeld, available, additional);

        if (available.compareTo(additional) < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        // Find previous top hold (exclude current user) BEFORE we modify/create the user's hold.
        Optional<Hold> prevTopBeforeOpt = holdRepository
                .findTopByAuctionIdAndStatusAndUserIdNotOrderByAmountDescCreatedAtDesc(auctionId, "HELD", userId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Create Bid history record (kept)
        Bid bid = Bid.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .userId(userId)
                .amount(amount)
                .createdAt(now)
                .build();
        bidRepository.save(bid);
        flushEm("after-bid-save");

        // Merge existing holds for this user+auction if multiple exist
        List<Hold> holdsForUserAuction = heldByUserAll.stream()
                .filter(h -> auctionId.equals(h.getAuctionId()))
                .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                .collect(Collectors.toList());

        Hold userHold = null;
        if (holdsForUserAuction.size() > 1) {
            // Merge: sum amounts, keep newest
            BigDecimal sumAmount = holdsForUserAuction.stream()
                    .map(h -> h.getAmount() == null ? BigDecimal.ZERO : h.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Hold newest = holdsForUserAuction.get(0);
            newest.setAmount(sumAmount);
            newest.setStatus("HELD");
            newest.setUpdatedAt(now);
            newest = persistAndFlushHold(newest);

            // delete older holds
            for (int i = 1; i < holdsForUserAuction.size(); i++) {
                Hold old = holdsForUserAuction.get(i);
                try {
                    holdRepository.delete(old);
                    flushEm("deleted-old-hold-" + old.getId());
                } catch (Throwable ex) {
                    log.warn("Failed to delete duplicate hold {}: {}", old.getId(), ex.getMessage());
                }
            }
            userHold = newest;
        } else if (holdsForUserAuction.size() == 1) {
            userHold = holdsForUserAuction.get(0);
        }

        // Now increase existing hold or create a new hold
        if (userHold != null) {
            if (additional.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal prevAmount = userHold.getAmount() == null ? BigDecimal.ZERO : userHold.getAmount();
                userHold.setAmount(prevAmount.add(additional));
                userHold.setStatus("HELD");
                userHold.setUpdatedAt(now);
                userHold = persistAndFlushHold(userHold);

                // IMPORTANT: do NOT deduct user.balance here.
                // Just create EscrowEntry/Transaction records to show hold; balance remains unchanged until finalize.

                EscrowEntry holdEntry = EscrowEntry.builder()
                        .id(UUID.randomUUID())
                        .saleId(null)
                        .userId(userId)
                        .amount(additional)
                        .type("HOLD")
                        .relatedEntity("HOLD")
                        .referenceId(userHold.getId())
                        .createdAt(now)
                        .build();
                escrowEntryRepository.save(holdEntry);

                Transaction holdTx = Transaction.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .type(TransactionType.HOLD)
                        .amount(additional)
                        .referenceId(userHold.getId())
                        .relatedEntity("HOLD")
                        .status("COMPLETED")
                        .createdAt(now)
                        .direction("OUT") // logical OUT for hold, but balance not physically changed yet
                        .description("Hold additional for increased bid")
                        .build();
                transactionRepository.save(holdTx);

                log.debug("Increased userHold {} by {} for user {}", userHold.getId(), additional, userId);
            } else {
                // no additional required - ensure hold stays HELD and updatedAt set
                userHold.setStatus("HELD");
                userHold.setUpdatedAt(now);
                userHold = persistAndFlushHold(userHold);
                log.debug("User re-bid without additional amount; existing hold {} unchanged", userHold.getId());
            }
        } else {
            // create a new hold for the bidder with the full amount
            userHold = Hold.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .auctionId(auctionId)
                    .amount(amount)
                    .status("HELD")
                    .createdAt(now)
                    .build();
            userHold = persistAndFlushHold(userHold);

            // Do not deduct user.balance here; create EscrowEntry/Transaction to represent hold.
            EscrowEntry holdEntry = EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(null)
                    .userId(userId)
                    .amount(amount)
                    .type("HOLD")
                    .relatedEntity("HOLD")
                    .referenceId(userHold.getId())
                    .createdAt(now)
                    .build();
            escrowEntryRepository.save(holdEntry);

            Transaction holdTx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .type(TransactionType.HOLD)
                    .amount(amount)
                    .referenceId(userHold.getId())
                    .relatedEntity("HOLD")
                    .status("COMPLETED")
                    .createdAt(now)
                    .direction("OUT")
                    .description("Hold for bid")
                    .build();
            transactionRepository.save(holdTx);

            log.debug("Created new hold {} amount={} for user {}", userHold.getId(), amount, userId);
        }

        // At this point userHold persisted. Now release/refund previous top (if any)
        if (prevTopBeforeOpt.isPresent()) {
            Hold prev = prevTopBeforeOpt.get();
            // reload prev from DB to ensure current state (and lock its user)
            Hold prevFromDb = holdRepository.findById(prev.getId()).orElse(null);
            if (prevFromDb != null && !"RELEASED".equalsIgnoreCase(prevFromDb.getStatus())
                    && !prevFromDb.getUserId().equals(userId)) {
                // Only mark prev RELEASED — do NOT refund user.balance since balance was not deducted at hold time.
                prevFromDb.setStatus("RELEASED");
                prevFromDb.setReleasedAt(now);
                prevFromDb = holdRepository.save(prevFromDb);
                flushEm("after-prev-release-" + prevFromDb.getId());

                // Create escrow REFUND entry and RELEASE transaction for bookkeeping, but DO NOT change user.balance.
                User prevUser = em.find(User.class, prevFromDb.getUserId(), LockModeType.PESSIMISTIC_WRITE);
                // NOTE: do NOT update prevUser.balance

                EscrowEntry refundEntry = EscrowEntry.builder()
                        .id(UUID.randomUUID())
                        .saleId(null)
                        .userId(prevUser.getId())
                        .amount(prevFromDb.getAmount())
                        .type("REFUND")
                        .relatedEntity("HOLD")
                        .referenceId(prevFromDb.getId())
                        .createdAt(now)
                        .build();
                escrowEntryRepository.save(refundEntry);

                Transaction relTx = Transaction.builder()
                        .id(UUID.randomUUID())
                        .userId(prevUser.getId())
                        .type(TransactionType.RELEASE)
                        .amount(prevFromDb.getAmount())
                        .referenceId(prevFromDb.getId())
                        .relatedEntity("HOLD")
                        .status("COMPLETED")
                        .createdAt(now)
                        .direction("IN")
                        .description("Hold released due to being outbid (no balance was charged at hold time)")
                        .build();
                transactionRepository.save(relTx);

                log.info("Released previous hold {} (user {}) amount={} — no balance refunded because hold didn't deduct balance",
                        prevFromDb.getId(), prevUser.getId(), prevFromDb.getAmount());
            } else {
                log.debug("Previous top hold {} not present or already released or belongs to current user - skip release",
                        prevTopBeforeOpt.map(Hold::getId).orElse(null));
            }
        }

        // Update auction current price
        auction.setCurrentPrice(amount);
        auctionRepository.save(auction);
        flushEm("after-auction-update");

        log.info("Bid placed: auction={} user={} amount={} (additional held={})", auctionId, userId, amount, additional);
        return bid;
    }

    /**
     * Finalize auctions that have ended.
     * Now we will CHARGE the winner (deduct winner.balance) at finalize time.
     */
    @Override
    @Transactional
    public void finalizeEndedAuctions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Auction> ended = auctionRepository.findByEndAtBeforeAndSettledFalse(now);
        for (Auction a : ended) {
            try {
                finalizeSingleAuction(a.getId());
            } catch (Exception ex) {
                log.error("Error finalizing auction {}", a.getId(), ex);
            }
        }
    }

    @Override
    @Transactional
    public void finalizeAuction(UUID auctionId) {
        finalizeSingleAuction(auctionId);
    }

    // extract finalization to single method to avoid duplication
    private void finalizeSingleAuction(UUID auctionId) {
        Auction auction = em.find(Auction.class, auctionId, LockModeType.PESSIMISTIC_WRITE);
        if (auction == null) return;

        Optional<Bid> topBidOpt = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtDesc(auction.getId());
        if (topBidOpt.isEmpty()) {
            auction.setStatus("FINISHED");
            auction.setSettled(true);
            auctionRepository.save(auction);
            log.info("Auction finished with no bids: {}", auction.getId());
            return;
        }

        Bid topBid = topBidOpt.get();
        UUID winnerId = topBid.getUserId();
        BigDecimal finalPrice = topBid.getAmount();
        OffsetDateTime now = OffsetDateTime.now();

        // Find winner hold (most recent HELD by winner on this auction)
        Optional<Hold> winnerHoldOpt = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "HELD")
                .filter(h -> h.getUserId().equals(winnerId) && h.getAmount().compareTo(finalPrice) >= 0);

        if (winnerHoldOpt.isEmpty()) {
            // no matching hold; set finished and require manual review
            auction.setStatus("FINISHED");
            auction.setSettled(true);
            auction.setWinnerId(winnerId);
            auction.setFinalPrice(finalPrice);
            auctionRepository.save(auction);
            log.warn("No matching hold found for winner {} on auction {}; manual review needed", winnerId, auction.getId());
            return;
        }

        Hold winnerHold = winnerHoldOpt.get();

        // At finalize time: charge the winner (deduct balance)
        User winner = em.find(User.class, winnerId, LockModeType.PESSIMISTIC_WRITE);
        BigDecimal winnerBal = winner.getBalance() == null ? BigDecimal.ZERO : winner.getBalance();
        if (winnerBal.compareTo(finalPrice) < 0) {
            // Not enough funds at settle time: mark for manual review or throw
            log.error("Winner {} has insufficient funds to settle auction {}: balance={} required={}", winnerId, auctionId, winnerBal, finalPrice);
            // Option A: mark finished and require manual review
            auction.setStatus("FINISHED");
            auction.setSettled(true);
            auction.setWinnerId(winnerId);
            auction.setFinalPrice(finalPrice);
            auctionRepository.save(auction);
            // Optionally notify admin / enqueue manual review
            return;
        }

        // Deduct winner balance now
        winner.setBalance(winnerBal.subtract(finalPrice));
        userRepository.save(winner);
        flushEm("after-winner-charge-" + winner.getId());

        // Create Sale record (ESCROWED)
        BigDecimal commissionRate = auction.getCommissionRate() == null ? BigDecimal.valueOf(5.00) : auction.getCommissionRate();
        BigDecimal commissionAmount = finalPrice.multiply(commissionRate).divide(BigDecimal.valueOf(100));
        BigDecimal netAmount = finalPrice.subtract(commissionAmount);

        Sale sale = Sale.builder()
                .id(UUID.randomUUID())
                .auctionId(auction.getId())
                .buyerId(winnerId)
                .sellerId(auction.getSellerId())
                .finalPrice(finalPrice)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .status("ESCROWED")
                .createdAt(OffsetDateTime.now())
                .build();
        saleRepository.save(sale);

        // Mark winner hold as USED and link via escrow entry
        winnerHold.setStatus("USED");
        winnerHold.setReleasedAt(OffsetDateTime.now());
        holdRepository.save(winnerHold);

        // Create escrow entry ESCROW_IN referencing sale (money moved from buyer into escrow)
        escrowEntryRepository.save(EscrowEntry.builder()
                .id(UUID.randomUUID())
                .saleId(sale.getId())
                .userId(winnerId)
                .amount(finalPrice)
                .type("ESCROW_IN")
                .relatedEntity("SALE")
                .referenceId(sale.getId())
                .createdAt(OffsetDateTime.now())
                .build());

        // Record transaction for the settlement (buyer charged)
        Transaction chargeTx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(winnerId)
                // Using RELEASE as placeholder type; replace with appropriate enum (e.g., CHARGE) if available
                .type(TransactionType.RELEASE)
                .amount(finalPrice)
                .referenceId(sale.getId())
                .relatedEntity("SALE")
                .status("COMPLETED")
                .createdAt(OffsetDateTime.now())
                .direction("OUT")
                .description("Charge winner for auction finalization")
                .build();
        transactionRepository.save(chargeTx);

        // Create payout record (PENDING) for seller
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .auctionId(auction.getId())
                .sellerId(auction.getSellerId())
                .totalAmount(finalPrice)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        payoutRepository.save(payout);

        // Create commission log
        CommissionLogs cl = CommissionLogs.builder()
                .id(UUID.randomUUID())
                .auctionId(auction.getId())
                .payoutId(payout.getId())
                .sellerId(auction.getSellerId())
                .commissionAmount(commissionAmount)
                .commissionRate(commissionRate)
                .createdAt(OffsetDateTime.now())
                .build();
        commissionLogsRepository.save(cl);

        // Update platform balance (credit commission)
        PlatformBalance pb = platformBalanceRepository.findAll().stream().findFirst().orElse(null);
        if (pb != null) {
            if (pb.getBalance() == null) pb.setBalance(BigDecimal.ZERO);
            if (pb.getTotalCommission() == null) pb.setTotalCommission(BigDecimal.ZERO);
            pb.setBalance(pb.getBalance().add(commissionAmount));
            pb.setTotalCommission(pb.getTotalCommission().add(commissionAmount));
            pb.setLastUpdated(OffsetDateTime.now());
            platformBalanceRepository.save(pb);
        }

        // Release other held holds (they were never deducted so no refund of balance needed)
        List<Hold> holds = holdRepository.findByAuctionIdOrderByCreatedAtDesc(auction.getId());
        for (Hold h : holds) {
            if (h.getId().equals(winnerHold.getId())) continue;
            if (!"HELD".equalsIgnoreCase(h.getStatus())) continue;
            h.setStatus("RELEASED");
            h.setReleasedAt(OffsetDateTime.now());
            holdRepository.save(h);

            // Do NOT modify user balances here because we didn't deduct on hold creation.
            // Create bookkeeping escrow & transaction entries for release.
            escrowEntryRepository.save(EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(sale.getId())
                    .userId(h.getUserId())
                    .amount(h.getAmount())
                    .type("REFUND")
                    .relatedEntity("HOLD")
                    .referenceId(h.getId())
                    .createdAt(OffsetDateTime.now())
                    .build());

            Transaction relTx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(h.getUserId())
                    .type(TransactionType.RELEASE)
                    .amount(h.getAmount())
                    .referenceId(h.getId())
                    .relatedEntity("HOLD")
                    .status("COMPLETED")
                    .createdAt(OffsetDateTime.now())
                    .direction("IN")
                    .description("Hold released after auction finalized (no balance previously charged)")
                    .build();
            transactionRepository.save(relTx);
        }

        // Update auction record
        auction.setStatus("FINISHED");
        auction.setSettled(true);
        auction.setWinnerId(winnerId);
        auction.setFinalPrice(finalPrice);
        auction.setCommissionAmount(commissionAmount);
        auctionRepository.save(auction);

        log.info("Auction finalized: auction={} winner={} finalPrice={} saleId={}", auction.getId(), winnerId, finalPrice, sale.getId());
    }
}