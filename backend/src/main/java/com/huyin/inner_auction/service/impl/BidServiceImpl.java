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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service xử lý đặt giá và finalize auctions.
 *
 * Vietnamese:
 * - placeBid: trừ ngay tiền của bidder (hold), tạo hold record và transaction HOLD,
 *   refund previous highest bidder nếu bị outbid.
 * - finalizeEndedAuctions: khi auction kết thúc, tạo Sale (ESCROWED), chuyển hold của winner
 *   thành used/escrow, tạo payout/commission_log, cập nhật platform_balance và mark auction settled.
 *
 * Lưu ý: tất cả thao tác tài chính chạy trong transaction (atomic).
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

    /**
     * Place a bid:
     * - lock auction and user row
     * - check auction active and bid >= current + minIncrement
     * - check available = balance - sum(HELD)
     * - deduct balance immediately, create hold, escrow entry, transaction HOLD
     * - release previous highest hold (status RELEASED), refund previous user (balance += amount), create RELEASE transaction and escrow REFUND
     */
    @Override
    @Transactional
    public Bid placeBid(UUID userId, UUID auctionId, BigDecimal amount) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        if (auctionId == null) throw new IllegalArgumentException("auctionId required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        // Lock auction row
        Auction auction = em.find(Auction.class, auctionId, LockModeType.PESSIMISTIC_WRITE);
        if (auction == null) throw new IllegalStateException("auction not found");
        OffsetDateTime now = OffsetDateTime.now();
        if (!"PUBLISHED".equalsIgnoreCase(auction.getStatus())) {
            throw new IllegalStateException("auction not open for bidding");
        }
        if (auction.getStartAt().isAfter(now) || auction.getEndAt().isBefore(now)) {
            throw new IllegalStateException("auction not active");
        }

        BigDecimal current = auction.getCurrentPrice() == null ? auction.getStartingPrice() : auction.getCurrentPrice();
        BigDecimal requiredMin = current.add(auction.getMinIncrement());
        if (amount.compareTo(requiredMin) < 0) {
            throw new IllegalArgumentException("bid too low; minimum is " + requiredMin);
        }

        // Lock user row
        User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new IllegalStateException("user not found");

        // Compute available = balance - sum of HELD holds
        List<Hold> heldByUser = holdRepository.findByUserIdAndStatus(userId, "HELD");
        BigDecimal totalHeld = heldByUser.stream().map(Hold::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal available = balance.subtract(totalHeld);
        if (available.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        // Create Bid
        Bid bid = Bid.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .userId(userId)
                .amount(amount)
                .createdAt(OffsetDateTime.now())
                .build();
        bidRepository.save(bid);

        // Deduct buyer balance immediately for this hold
        user.setBalance(balance.subtract(amount));
        userRepository.save(user);

        // Create Hold record
        Hold hold = Hold.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .auctionId(auctionId)
                .amount(amount)
                .status("HELD")
                .createdAt(OffsetDateTime.now())
                .build();
        holdRepository.save(hold);

        // Create EscrowEntry for audit (HOLD)
        EscrowEntry holdEntry = EscrowEntry.builder()
                .id(UUID.randomUUID())
                .saleId(null)
                .userId(userId)
                .amount(amount)
                .type("HOLD")
                .relatedEntity("HOLD")
                .referenceId(hold.getId())
                .build();
        escrowEntryRepository.save(holdEntry);

        // Create Transaction HOLD
        Transaction holdTx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.HOLD)
                .amount(amount)
                .referenceId(hold.getId())
                .relatedEntity("HOLD")
                .status("COMPLETED")
                .createdAt(OffsetDateTime.now())
                .direction("OUT")
                .description("Hold for bid")
                .build();
        transactionRepository.save(holdTx);

        // Release previous highest hold if exists and belongs to different user
        Optional<Hold> prevOpt = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(auctionId, "HELD");
        if (prevOpt.isPresent()) {
            Hold prev = prevOpt.get();
            if (!prev.getId().equals(hold.getId()) && !prev.getUserId().equals(userId)) {
                // Mark prev RELEASED
                prev.setStatus("RELEASED");
                prev.setReleasedAt(OffsetDateTime.now());
                holdRepository.save(prev);

                // Refund previous user: credit back balance
                User prevUser = em.find(User.class, prev.getUserId(), LockModeType.PESSIMISTIC_WRITE);
                BigDecimal prevBal = prevUser.getBalance() == null ? BigDecimal.ZERO : prevUser.getBalance();
                prevUser.setBalance(prevBal.add(prev.getAmount()));
                userRepository.save(prevUser);

                // Escrow REFUND entry
                EscrowEntry refundEntry = EscrowEntry.builder()
                        .id(UUID.randomUUID())
                        .saleId(null)
                        .userId(prevUser.getId())
                        .amount(prev.getAmount())
                        .type("REFUND")
                        .relatedEntity("HOLD")
                        .referenceId(prev.getId())
                        .build();
                escrowEntryRepository.save(refundEntry);

                // Create RELEASE transaction for previous holder
                Transaction relTx = Transaction.builder()
                        .id(UUID.randomUUID())
                        .userId(prevUser.getId())
                        .type(TransactionType.RELEASE)
                        .amount(prev.getAmount())
                        .referenceId(prev.getId())
                        .relatedEntity("HOLD")
                        .status("COMPLETED")
                        .createdAt(OffsetDateTime.now())
                        .direction("IN")
                        .description("Refund due to outbid")
                        .build();
                transactionRepository.save(relTx);
            }
        }

        // Update auction current price
        auction.setCurrentPrice(amount);
        auctionRepository.save(auction);

        log.info("Bid placed: auction={} user={} amount={} (balance deducted)", auctionId, userId, amount);
        return bid;
    }

    /**
     * Finalize auctions that have ended:
     * - find ended & unsettled auctions
     * - determine top bid
     * - create Sale (ESCROWED) converting winner hold into sale, create commission_logs, payout (PENDING)
     * - update platform_balance with commission
     * - release other holds to their owners (refund)
     */
    @Override
    @Transactional
    public void finalizeEndedAuctions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Auction> ended = auctionRepository.findByEndAtBeforeAndSettledFalse(now);
        for (Auction a : ended) {
            try {
                Auction auction = em.find(Auction.class, a.getId(), LockModeType.PESSIMISTIC_WRITE);
                if (auction == null) continue;

                Optional<Bid> topBidOpt = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtDesc(auction.getId());
                if (topBidOpt.isEmpty()) {
                    // no bids -> finish
                    auction.setStatus("FINISHED");
                    auction.setSettled(true);
                    auctionRepository.save(auction);
                    log.info("Auction finished with no bids: {}", auction.getId());
                    continue;
                }

                Bid topBid = topBidOpt.get();
                UUID winnerId = topBid.getUserId();
                BigDecimal finalPrice = topBid.getAmount();

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
                    continue;
                }

                Hold winnerHold = winnerHoldOpt.get();

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

                // Create escrow entry ESCROW_IN referencing sale
                escrowEntryRepository.save(EscrowEntry.builder()
                        .id(UUID.randomUUID())
                        .saleId(sale.getId())
                        .userId(winnerId)
                        .amount(finalPrice)
                        .type("ESCROW_IN")
                        .relatedEntity("SALE")
                        .referenceId(sale.getId())
                        .build());

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

                // Release other held holds (refund to their users)
                List<Hold> holds = holdRepository.findByAuctionIdOrderByCreatedAtDesc(auction.getId());
                for (Hold h : holds) {
                    if (h.getId().equals(winnerHold.getId())) continue;
                    if (!"HELD".equalsIgnoreCase(h.getStatus())) continue;
                    h.setStatus("RELEASED");
                    h.setReleasedAt(OffsetDateTime.now());
                    holdRepository.save(h);

                    // Refund user balance
                    User u = em.find(User.class, h.getUserId(), LockModeType.PESSIMISTIC_WRITE);
                    BigDecimal uBal = u.getBalance() == null ? BigDecimal.ZERO : u.getBalance();
                    u.setBalance(uBal.add(h.getAmount()));
                    userRepository.save(u);

                    // Create escrow REFUND entry
                    escrowEntryRepository.save(EscrowEntry.builder()
                            .id(UUID.randomUUID())
                            .saleId(sale.getId())
                            .userId(u.getId())
                            .amount(h.getAmount())
                            .type("REFUND")
                            .relatedEntity("HOLD")
                            .referenceId(h.getId())
                            .build());

                    // Create RELEASE transaction
                    Transaction relTx = Transaction.builder()
                            .id(UUID.randomUUID())
                            .userId(u.getId())
                            .type(TransactionType.RELEASE)
                            .amount(h.getAmount())
                            .referenceId(h.getId())
                            .relatedEntity("HOLD")
                            .status("COMPLETED")
                            .createdAt(OffsetDateTime.now())
                            .direction("IN")
                            .description("Refund after auction finalized")
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
            } catch (Exception ex) {
                log.error("Error finalizing auction {}", a.getId(), ex);
                // do not throw to allow processing other auctions; admin can inspect failure
            }
        }
    }

    // Add this method inside your existing BidServiceImpl class (the class that already has finalizeEndedAuctions)
    @Override
    @Transactional
    public void finalizeAuction(UUID auctionId) {
        if (auctionId == null) throw new IllegalArgumentException("auctionId required");
        Auction a = em.find(Auction.class, auctionId, LockModeType.PESSIMISTIC_WRITE);
        if (a == null) throw new IllegalStateException("auction not found");
        // reuse logic from finalizeEndedAuctions for a single auction
        // For simplicity, call the same logic by temporary creating a list with this auction and processing it.
        // You can refactor the core finalize logic into a private method finalizeSingleAuction(Auction auction)
        // and call it here. For brevity, we'll call finalizeEndedAuctions() after adjusting a.endAt to now - but better to extract method.
        // Here we'll implement using the same steps as finalizeEndedAuctions but only for given auction:
        try {
            // Find top bid
            Optional<Bid> topBidOpt = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtDesc(a.getId());
            if (topBidOpt.isEmpty()) {
                a.setStatus("FINISHED");
                a.setSettled(true);
                auctionRepository.save(a);
                return;
            }
            Bid topBid = topBidOpt.get();
            UUID winnerId = topBid.getUserId();
            java.math.BigDecimal finalPrice = topBid.getAmount();

            Optional<Hold> winnerHoldOpt = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(a.getId(), "HELD")
                    .filter(h -> h.getUserId().equals(winnerId) && h.getAmount().compareTo(finalPrice) >= 0);

            if (winnerHoldOpt.isEmpty()) {
                a.setStatus("FINISHED");
                a.setSettled(true);
                a.setWinnerId(winnerId);
                a.setFinalPrice(finalPrice);
                auctionRepository.save(a);
                throw new IllegalStateException("no matching hold for winner; manual review required");
            }

            Hold winnerHold = winnerHoldOpt.get();

            // Create Sale record
            java.math.BigDecimal commissionRate = a.getCommissionRate() == null ? java.math.BigDecimal.valueOf(5.00) : a.getCommissionRate();
            java.math.BigDecimal commissionAmount = finalPrice.multiply(commissionRate).divide(java.math.BigDecimal.valueOf(100));
            java.math.BigDecimal netAmount = finalPrice.subtract(commissionAmount);

            Sale sale = Sale.builder()
                    .id(UUID.randomUUID())
                    .auctionId(a.getId())
                    .buyerId(winnerId)
                    .sellerId(a.getSellerId())
                    .finalPrice(finalPrice)
                    .commissionRate(commissionRate)
                    .commissionAmount(commissionAmount)
                    .netAmount(netAmount)
                    .status("ESCROWED")
                    .createdAt(OffsetDateTime.now())
                    .build();
            saleRepository.save(sale);

            // Mark winner hold USED
            winnerHold.setStatus("USED");
            winnerHold.setReleasedAt(OffsetDateTime.now());
            holdRepository.save(winnerHold);

            // Escrow entry
            escrowEntryRepository.save(EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(sale.getId())
                    .userId(winnerId)
                    .amount(finalPrice)
                    .type("ESCROW_IN")
                    .relatedEntity("SALE")
                    .referenceId(sale.getId())
                    .build());

            // Payout + commission log + platform balance update same as finalizeEndedAuctions
            Payout payout = Payout.builder()
                    .id(UUID.randomUUID())
                    .auctionId(a.getId())
                    .sellerId(a.getSellerId())
                    .totalAmount(finalPrice)
                    .commissionAmount(commissionAmount)
                    .netAmount(netAmount)
                    .status("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .build();
            payoutRepository.save(payout);

            CommissionLogs cl = CommissionLogs.builder()
                    .id(UUID.randomUUID())
                    .auctionId(a.getId())
                    .payoutId(payout.getId())
                    .sellerId(a.getSellerId())
                    .commissionAmount(commissionAmount)
                    .commissionRate(commissionRate)
                    .createdAt(OffsetDateTime.now())
                    .build();
            commissionLogsRepository.save(cl);

            PlatformBalance pb = platformBalanceRepository.findAll().stream().findFirst().orElse(null);
            if (pb != null) {
                if (pb.getBalance() == null) pb.setBalance(java.math.BigDecimal.ZERO);
                if (pb.getTotalCommission() == null) pb.setTotalCommission(java.math.BigDecimal.ZERO);
                pb.setBalance(pb.getBalance().add(commissionAmount));
                pb.setTotalCommission(pb.getTotalCommission().add(commissionAmount));
                pb.setLastUpdated(OffsetDateTime.now());
                platformBalanceRepository.save(pb);
            }

            // Refund other holds
            List<Hold> holds = holdRepository.findByAuctionIdOrderByCreatedAtDesc(a.getId());
            for (Hold h : holds) {
                if (h.getId().equals(winnerHold.getId())) continue;
                if (!"HELD".equalsIgnoreCase(h.getStatus())) continue;
                h.setStatus("RELEASED");
                h.setReleasedAt(OffsetDateTime.now());
                holdRepository.save(h);

                User u = em.find(User.class, h.getUserId(), LockModeType.PESSIMISTIC_WRITE);
                java.math.BigDecimal uBal = u.getBalance() == null ? java.math.BigDecimal.ZERO : u.getBalance();
                u.setBalance(uBal.add(h.getAmount()));
                userRepository.save(u);

                escrowEntryRepository.save(EscrowEntry.builder()
                        .id(UUID.randomUUID())
                        .saleId(sale.getId())
                        .userId(u.getId())
                        .amount(h.getAmount())
                        .type("REFUND")
                        .relatedEntity("HOLD")
                        .referenceId(h.getId())
                        .build());

                Transaction relTx = Transaction.builder()
                        .id(UUID.randomUUID())
                        .userId(u.getId())
                        .type(TransactionType.RELEASE)
                        .amount(h.getAmount())
                        .referenceId(h.getId())
                        .relatedEntity("HOLD")
                        .status("COMPLETED")
                        .createdAt(OffsetDateTime.now())
                        .direction("IN")
                        .description("Refund after auction finalized")
                        .build();
                transactionRepository.save(relTx);
            }

            a.setStatus("FINISHED");
            a.setSettled(true);
            a.setWinnerId(winnerId);
            a.setFinalPrice(finalPrice);
            a.setCommissionAmount(commissionAmount);
            auctionRepository.save(a);

        } catch (Exception ex) {
            log.error("Error finalizing auction {}", auctionId, ex);
            throw ex;
        }
    }
}