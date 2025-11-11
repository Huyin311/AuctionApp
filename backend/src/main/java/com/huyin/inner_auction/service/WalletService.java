package com.huyin.inner_auction.service;

import com.huyin.inner_auction.dto.WalletDto;
import com.huyin.inner_auction.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    BigDecimal topUp(UUID userId, BigDecimal amount);
    BigDecimal getBalance(UUID userId);
    List<Transaction> listTransactions(UUID userId);
    WalletDto getWalletByUserId(UUID userId);
}