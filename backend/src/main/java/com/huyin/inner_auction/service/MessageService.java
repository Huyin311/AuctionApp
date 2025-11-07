package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Message;

import java.util.List;
import java.util.UUID;

/**
 * Service cho message (nhắn tin giữa người dùng)
 *
 * Vietnamese:
 * - sendMessage: gửi tin nhắn (có thể liên quan auction hoặc sale)
 * - getMessagesByAuction / getMessagesBySale: lấy lịch sử tin nhắn
 * - getConversation: lấy tin nhắn giữa 2 user
 * - getUserMessages: inbox + outbox cho 1 user
 */
public interface MessageService {
    Message sendMessage(UUID fromUserId, UUID toUserId, UUID auctionId, UUID saleId, String content);

    List<Message> getMessagesByAuction(UUID auctionId);

    List<Message> getMessagesBySale(UUID saleId);

    List<Message> getConversation(UUID userA, UUID userB);

    List<Message> getUserMessages(UUID userId);
}