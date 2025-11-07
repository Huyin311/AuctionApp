package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    // Tin nhắn theo auction (sắp xếp theo thời gian tăng dần)
    List<Message> findByAuctionIdOrderByCreatedAtAsc(UUID auctionId);

    // Tin nhắn theo sale (sau khi auction kết thúc)
    List<Message> findBySaleIdOrderByCreatedAtAsc(UUID saleId);

    // Tin nhắn giữa hai user (cả hai chiều), sắp xếp theo thời gian tăng dần
    List<Message> findByFromUserAndToUserOrderByCreatedAtAsc(UUID fromUser, UUID toUser);
    List<Message> findByToUserAndFromUserOrderByCreatedAtAsc(UUID toUser, UUID fromUser);

    // Lấy tất cả tin nhắn liên quan tới một user (inbox/outbox) theo thời gian giảm dần
    List<Message> findByFromUserOrToUserOrderByCreatedAtDesc(UUID fromUser, UUID toUser);
}