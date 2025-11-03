package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Transaction;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.TransactionRepository;
import com.huyin.inner_auction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wallet operations (mock top-up).
 */
@Service
public class WalletService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }


    @Transactional
    public void topUp(UUID userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy balance hiện tại, nếu null thì BigDecimal.ZERO
        BigDecimal currentBal = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal topUpAmount = BigDecimal.valueOf(amount);
        BigDecimal newBal = currentBal.add(topUpAmount);

        user.setBalance(newBal);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type("TOPUP")
                .amount(topUpAmount)  // dùng BigDecimal
                .referenceId(null)
                .relatedEntity("wallet")
                .status("COMPLETED")
                .createdAt(OffsetDateTime.now())
                .build();

        transactionRepository.save(tx);
    }

    public User getProfile(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }
}