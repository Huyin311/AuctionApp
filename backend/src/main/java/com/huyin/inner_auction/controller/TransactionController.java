package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Transaction;
import com.huyin.inner_auction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API cho lịch sử giao dịch của user (wallet).
 * GET /api/wallet/transactions?page=&size=&type=
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class TransactionController {

//    private final TransactionRepository transactionRepository;
//
//    @GetMapping("/transactions")
//    public ResponseEntity<?> listTransactions(Authentication authentication,
//                                              @RequestParam(value = "page", defaultValue = "0") int page,
//                                              @RequestParam(value = "size", defaultValue = "20") int size,
//                                              @RequestParam(value = "type", required = false) String type) {
//        if (authentication == null) return ResponseEntity.status(401).build();
//        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
//        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
//        Page<Transaction> pageResult;
//        if (type != null && !type.isBlank()) {
//            pageResult = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
//        } else {
//            pageResult = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
//        }
//        return ResponseEntity.ok(pageResult);
//    }
}