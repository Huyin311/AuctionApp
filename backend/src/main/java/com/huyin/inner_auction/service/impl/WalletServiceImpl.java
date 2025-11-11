package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.dto.WalletDto;
import com.huyin.inner_auction.entity.Transaction;
import com.huyin.inner_auction.entity.TransactionType;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.TransactionRepository;
import com.huyin.inner_auction.repository.UserRepository;
import com.huyin.inner_auction.service.WalletService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final JdbcTemplate jdbcTemplate;


    public WalletServiceImpl(UserRepository userRepository, TransactionRepository transactionRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.jdbcTemplate = jdbcTemplate;
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
    @Override
    public WalletDto getWalletByUserId(UUID userId) {
        if (userId == null) return null;
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) return null;
        User u = opt.get();

        BigDecimal balance = u.getBalance() == null ? BigDecimal.ZERO : u.getBalance();

        // Sum holds with status 'HELD' for this user from holds table (native)
        BigDecimal held = BigDecimal.ZERO;
        try {
            Object res = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount),0) FROM holds WHERE user_id = ? AND status = ?",
                    new Object[]{userId, "HELD"},
                    BigDecimal.class
            );
            if (res != null) held = (BigDecimal) res;
        } catch (Exception ex) {
            // Log if you have logging, but swallow here and assume held=0
            // e.g., log.warn("Failed to sum holds for user {}", userId, ex);
            held = BigDecimal.ZERO;
        }

        BigDecimal available = balance.subtract(held == null ? BigDecimal.ZERO : held);
        if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;

        return WalletDto.builder()
                .userId(u.getId())
                .balance(balance)
                .heldAmount(held)
                .available(available)
                .build();
    }
}