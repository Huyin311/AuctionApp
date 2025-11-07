package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.entity.Message;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.MessageRepository;
import com.huyin.inner_auction.repository.UserRepository;
import com.huyin.inner_auction.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of MessageService
 *
 * Vietnamese:
 * - Ghi log tin nhắn vào bảng messages
 * - Kiểm tra tồn tại người gửi/người nhận (nếu toUserId được cung cấp)
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EntityManager em;

    @Override
    @Transactional
    public Message sendMessage(UUID fromUserId, UUID toUserId, UUID auctionId, UUID saleId, String content) {
        if (fromUserId == null) throw new IllegalArgumentException("fromUserId required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content required");

        // Optional: ensure fromUser exists and lock user row (prevent abuses)
        User from = em.find(User.class, fromUserId, LockModeType.PESSIMISTIC_READ);
        if (from == null) throw new IllegalStateException("sender not found");

        // If toUserId provided, validate exists
        if (toUserId != null) {
            User to = em.find(User.class, toUserId, LockModeType.PESSIMISTIC_READ);
            if (to == null) throw new IllegalStateException("recipient not found");
        }

        Message m = Message.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .saleId(saleId)
                .fromUser(fromUserId)
                .toUser(toUserId)
                .content(content)
                .createdAt(OffsetDateTime.now())
                .build();
        return messageRepository.save(m);
    }

    @Override
    public List<Message> getMessagesByAuction(UUID auctionId) {
        if (auctionId == null) return new ArrayList<>();
        return messageRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId);
    }

    @Override
    public List<Message> getMessagesBySale(UUID saleId) {
        if (saleId == null) return new ArrayList<>();
        return messageRepository.findBySaleIdOrderByCreatedAtAsc(saleId);
    }

    @Override
    public List<Message> getConversation(UUID userA, UUID userB) {
        if (userA == null || userB == null) return new ArrayList<>();
        // fetch two directions and merge/sort
        List<Message> aToB = messageRepository.findByFromUserAndToUserOrderByCreatedAtAsc(userA, userB);
        List<Message> bToA = messageRepository.findByFromUserAndToUserOrderByCreatedAtAsc(userB, userA);
        List<Message> merged = new ArrayList<>();
        merged.addAll(aToB);
        merged.addAll(bToA);
        merged.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
        return merged;
    }

    @Override
    public List<Message> getUserMessages(UUID userId) {
        if (userId == null) return new ArrayList<>();
        return messageRepository.findByFromUserOrToUserOrderByCreatedAtDesc(userId, userId);
    }
}