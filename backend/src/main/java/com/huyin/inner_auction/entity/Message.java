package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Message entity: lưu tin nhắn giữa người dùng (có thể liên quan auction hoặc sale)
 *
 * Vietnamese:
 * - auctionId: optional, nếu tin nhắn trong trang auction
 * - saleId: optional, nếu tin nhắn liên quan sale (sau khi auction kết thúc)
 * - fromUser, toUser: người gửi/nhận
 * - content: nội dung tin nhắn
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "auction_id")
    private UUID auctionId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "from_user", nullable = false)
    private UUID fromUser;

    @Column(name = "to_user")
    private UUID toUser;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}