package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.*;
import com.huyin.inner_auction.repository.*;
import com.huyin.inner_auction.service.DisputeService;
import com.huyin.inner_auction.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implementation cơ bản cho DisputeService.
 * - release: gọi SaleService.releaseFunds
 * - refund: trả lại buyer và set sale.status=REFUNDED
 * - split: tách tiền giữa seller và buyer (amountToSeller)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EscrowEntryRepository escrowEntryRepository;
    private final SaleService saleService;

    @Override
    @Transactional
    public void resolveDispute(UUID adminUserId, UUID disputeId, String action, BigDecimal amountToSeller, String note) {
        Dispute d = disputeRepository.findById(disputeId).orElseThrow(() -> new IllegalStateException("dispute not found"));
        if (!"OPEN".equalsIgnoreCase(d.getStatus()) && !"UNDER_REVIEW".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalStateException("dispute already resolved");
        }
        Sale sale = saleRepository.findById(d.getSaleId()).orElseThrow(() -> new IllegalStateException("sale not found"));

        if ("release".equalsIgnoreCase(action)) {
            // release funds to seller
            saleService.releaseFunds(adminUserId, sale.getId());
            d.setStatus("RESOLVED");
            d.setResolution("release: admin=" + adminUserId + " note=" + note);
            disputeRepository.save(d);
            return;
        }

        if ("refund".equalsIgnoreCase(action)) {
            // refund full amount to buyer
            BigDecimal refundAmount = sale.getFinalPrice();
            // credit buyer
            User buyer = userRepository.findById(sale.getBuyerId()).orElseThrow(() -> new IllegalStateException("buyer not found"));
            BigDecimal bal = buyer.getBalance() == null ? BigDecimal.ZERO : buyer.getBalance();
            buyer.setBalance(bal.add(refundAmount));
            userRepository.save(buyer);

            // transaction refund
            Transaction tx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(buyer.getId())
                    .type(TransactionType.REFUND)
                    .amount(refundAmount)
                    .referenceId(sale.getId())
                    .relatedEntity("SALE")
                    .status("COMPLETED")
                    .createdAt(OffsetDateTime.now())
                    .direction("IN")
                    .description("Refund by admin dispute resolution")
                    .build();
            transactionRepository.save(tx);

            // escrow entry
            escrowEntryRepository.save(EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(sale.getId())
                    .userId(buyer.getId())
                    .amount(refundAmount)
                    .type("REFUND")
                    .relatedEntity("DISPUTE")
                    .referenceId(d.getId())
                    .build());

            // mark sale refunded
            sale.setStatus("REFUNDED");
            saleRepository.save(sale);

            d.setStatus("RESOLVED");
            d.setResolution("refund: admin=" + adminUserId + " note=" + note);
            disputeRepository.save(d);
            return;
        }

        if ("split".equalsIgnoreCase(action)) {
            if (amountToSeller == null) throw new IllegalArgumentException("amountToSeller required for split");
            BigDecimal finalPrice = sale.getFinalPrice();
            if (amountToSeller.compareTo(BigDecimal.ZERO) < 0 || amountToSeller.compareTo(finalPrice) > 0)
                throw new IllegalArgumentException("invalid amountToSeller");

            BigDecimal amountToBuyer = finalPrice.subtract(amountToSeller);

            // credit seller
            User seller = userRepository.findById(sale.getSellerId()).orElseThrow(() -> new IllegalStateException("seller not found"));
            seller.setBalance((seller.getBalance() == null ? BigDecimal.ZERO : seller.getBalance()).add(amountToSeller));
            userRepository.save(seller);
            Transaction tSeller = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(seller.getId())
                    .type(TransactionType.PAYOUT)
                    .amount(amountToSeller)
                    .referenceId(sale.getId())
                    .relatedEntity("SALE")
                    .status("COMPLETED")
                    .createdAt(OffsetDateTime.now())
                    .direction("IN")
                    .description("Partial release by admin dispute split")
                    .build();
            transactionRepository.save(tSeller);

            // credit buyer
            User buyer = userRepository.findById(sale.getBuyerId()).orElseThrow(() -> new IllegalStateException("buyer not found"));
            buyer.setBalance((buyer.getBalance() == null ? BigDecimal.ZERO : buyer.getBalance()).add(amountToBuyer));
            userRepository.save(buyer);
            Transaction tBuyer = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(buyer.getId())
                    .type(TransactionType.REFUND)
                    .amount(amountToBuyer)
                    .referenceId(sale.getId())
                    .relatedEntity("SALE")
                    .status("COMPLETED")
                    .createdAt(OffsetDateTime.now())
                    .direction("IN")
                    .description("Partial refund by admin dispute split")
                    .build();
            transactionRepository.save(tBuyer);

            // escrow entries
            escrowEntryRepository.save(EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(sale.getId())
                    .userId(seller.getId())
                    .amount(amountToSeller)
                    .type("RELEASE_PARTIAL")
                    .relatedEntity("DISPUTE")
                    .referenceId(d.getId())
                    .build());
            escrowEntryRepository.save(EscrowEntry.builder()
                    .id(UUID.randomUUID())
                    .saleId(sale.getId())
                    .userId(buyer.getId())
                    .amount(amountToBuyer)
                    .type("REFUND_PARTIAL")
                    .relatedEntity("DISPUTE")
                    .referenceId(d.getId())
                    .build());

            sale.setStatus("REFUNDED"); // or PARTIAL; choose REFUNDED for simplicity
            saleRepository.save(sale);

            d.setStatus("RESOLVED");
            d.setResolution("split: admin=" + adminUserId + " note=" + note + " sellerAmount=" + amountToSeller);
            disputeRepository.save(d);
            return;
        }

        throw new IllegalArgumentException("unknown action");
    }
}