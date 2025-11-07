package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.Transaction;
import com.huyin.inner_auction.entity.TransactionType;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.TransactionRepository;
import com.huyin.inner_auction.repository.UserRepository;
import com.huyin.inner_auction.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public BigDecimal topUp(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("user not found"));
        BigDecimal current = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal newBalance = current.add(amount);
        user.setBalance(newBalance);
        userRepository.save(user);

        Transaction tx = Transaction.builder()
                .id(null)
                .userId(userId)
                .type(TransactionType.TOPUP)
                .amount(amount)
                .referenceId(null)
                .relatedEntity(null)
                .status("COMPLETED")
                .build();
        transactionRepository.save(tx);

        return newBalance;
    }

    @Override
    public BigDecimal getBalance(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("user not found"));
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    @Override
    public List<Transaction> listTransactions(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}