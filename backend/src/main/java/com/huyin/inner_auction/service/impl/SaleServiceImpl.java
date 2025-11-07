package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.*;
import com.huyin.inner_auction.repository.*;
import com.huyin.inner_auction.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of SaleService
 *
 * Vietnamese:
 * - releaseFunds thực hiện việc chuyển net_amount cho seller, tạo transaction, escrow entry và cập nhật platform_balance.
 * - confirmDelivery: buyer gọi -> ghi delivery confirmation và gọi releaseFunds.
 * - autoReleasePendingSales: scheduler gọi để auto release theo policy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final ShipmentRepository shipmentRepository;
    private final DeliveryConfirmationRepository deliveryConfirmationRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowEntryRepository escrowEntryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformBalanceRepository platformBalanceRepository;
    private final PayoutRepository payoutRepository;
    private final EntityManager em;

    @Override
    @Transactional
    public void confirmDelivery(UUID buyerId, UUID saleId) {
        Sale sale = em.find(Sale.class, saleId, LockModeType.PESSIMISTIC_WRITE);
        if (sale == null) throw new IllegalStateException("sale not found");
        if (!sale.getBuyerId().equals(buyerId)) throw new IllegalStateException("not buyer of this sale");
        if (!"ESCROWED".equalsIgnoreCase(sale.getStatus())) throw new IllegalStateException("sale not in ESCROWED state");

        // Check open disputes
        List<Dispute> disputes = disputeRepository.findBySaleId(saleId);
        boolean hasOpen = disputes.stream().anyMatch(d -> "OPEN".equalsIgnoreCase(d.getStatus()) || "UNDER_REVIEW".equalsIgnoreCase(d.getStatus()));
        if (hasOpen) throw new IllegalStateException("sale has open dispute");

        // Insert delivery confirmation
        DeliveryConfirmation dc = DeliveryConfirmation.builder()
                .saleId(saleId)
                .buyerId(buyerId)
                .note(null)
                .build();
        deliveryConfirmationRepository.save(dc);

        // Release funds
        releaseFunds(null, saleId); // admin/system user id optional
    }

    @Override
    @Transactional
    public Sale releaseFunds(UUID adminOrSystemUserId, UUID saleId) {
        Sale sale = em.find(Sale.class, saleId, LockModeType.PESSIMISTIC_WRITE);
        if (sale == null) throw new IllegalStateException("sale not found");
        if (!"ESCROWED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalStateException("sale status must be ESCROWED to release");
        }

        // Check disputes again
        List<Dispute> disputes = disputeRepository.findBySaleId(saleId);
        boolean hasOpen = disputes.stream().anyMatch(d -> "OPEN".equalsIgnoreCase(d.getStatus()) || "UNDER_REVIEW".equalsIgnoreCase(d.getStatus()));
        if (hasOpen) throw new IllegalStateException("sale has open dispute; cannot release");

        BigDecimal net = sale.getNetAmount() == null ? BigDecimal.ZERO : sale.getNetAmount();
        if (net.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("invalid net amount");

        // Credit seller
        User seller = em.find(User.class, sale.getSellerId(), LockModeType.PESSIMISTIC_WRITE);
        BigDecimal sellerBal = seller.getBalance() == null ? BigDecimal.ZERO : seller.getBalance();
        seller.setBalance(sellerBal.add(net));
        userRepository.save(seller);

        // Create transaction PAYOUT for seller
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(seller.getId())
                .type(TransactionType.PAYOUT)
                .amount(net)
                .referenceId(sale.getId())
                .relatedEntity("SALE")
                .status("COMPLETED")
                .createdAt(OffsetDateTime.now())
                .direction("IN")
                .description("Release funds for sale " + sale.getId())
                .build();
        transactionRepository.save(tx);

        // Create escrow entry RELEASE
        escrowEntryRepository.save(EscrowEntry.builder()
                .id(UUID.randomUUID())
                .saleId(sale.getId())
                .userId(seller.getId())
                .amount(net)
                .type("RELEASE")
                .relatedEntity("SALE")
                .referenceId(sale.getId())
                .build());

        // Update platform_balance: commission was already added at finalize; if not, handle here.
        PlatformBalance pb = platformBalanceRepository.findAll().stream().findFirst().orElse(null);
        if (pb != null) {
            pb.setLastUpdated(OffsetDateTime.now());
            platformBalanceRepository.save(pb);
        }

        // Mark sale released
        sale.setStatus("RELEASED");
        sale.setUpdatedAt(OffsetDateTime.now());
        saleRepository.save(sale);

        log.info("Sale released: saleId={} seller={} net={} by={}", sale.getId(), seller.getId(), net, adminOrSystemUserId);
        return sale;
    }

    @Override
    @Transactional
    public void autoReleasePendingSales() {
        // Policy example: release ESCROWED sales that have shipment.shipped_at + 14 days passed OR have delivery confirmation
        List<Sale> escrows = saleRepository.findByStatus("ESCROWED");
        for (Sale s : escrows) {
            try {
                // If delivery confirmation exists -> release
                boolean hasConfirm = em.createQuery("SELECT count(dc) FROM DeliveryConfirmation dc WHERE dc.saleId = :saleId", Long.class)
                        .setParameter("saleId", s.getId()).getSingleResult() > 0;
                if (hasConfirm) {
                    releaseFunds(null, s.getId());
                    continue;
                }
                // Else check shipment date
                Shipment sh = (Shipment) em.createQuery("SELECT sh FROM Shipment sh WHERE sh.saleId = :saleId")
                        .setParameter("saleId", s.getId())
                        .setMaxResults(1)
                        .getResultStream().findFirst().orElse(null);
                if (sh != null && sh.getShippedAt() != null) {
                    OffsetDateTime releaseAfter = sh.getShippedAt().plusDays(14); // policy: 14 days after shipped auto-release
                    if (OffsetDateTime.now().isAfter(releaseAfter)) {
                        releaseFunds(null, s.getId());
                    }
                }
            } catch (Exception ex) {
                log.error("autoRelease error for sale {}", s.getId(), ex);
            }
        }
    }
}