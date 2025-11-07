package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.entity.Message;
import com.huyin.inner_auction.service.MessageService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API cho messages.
 *
 * Endpoints:
 * - POST /api/messages            : gửi tin nhắn (auth required)
 * - GET  /api/auctions/{id}/messages : lấy message của auction
 * - GET  /api/sales/{id}/messages    : lấy message của sale
 * - GET  /api/messages/conversation?with={userId}  : lấy conversation giữa current user và with
 * - GET  /api/messages/me            : lấy inbox/outbox cho user
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Data
    static class SendMessageRequest {
        private String auctionId; // optional
        private String saleId;    // optional
        private String toUserId;  // optional
        @NotBlank(message = "content required")
        private String content;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(Authentication authentication, @RequestBody SendMessageRequest req) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID fromUserId = UUID.fromString(authentication.getPrincipal().toString());
        UUID auctionId = req.getAuctionId() == null ? null : UUID.fromString(req.getAuctionId());
        UUID saleId = req.getSaleId() == null ? null : UUID.fromString(req.getSaleId());
        UUID toUserId = req.getToUserId() == null ? null : UUID.fromString(req.getToUserId());
        try {
            Message m = messageService.sendMessage(fromUserId, toUserId, auctionId, saleId, req.getContent());
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "detail", ex.getMessage()));
        }
    }

    @GetMapping("/auctions/{auctionId}/messages")
    public ResponseEntity<?> getAuctionMessages(Authentication authentication, @PathVariable String auctionId) {
        // Authentication optional if messages public; here we allow both
        UUID aId = UUID.fromString(auctionId);
        List<Message> list = messageService.getMessagesByAuction(aId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/sales/{saleId}/messages")
    public ResponseEntity<?> getSaleMessages(Authentication authentication, @PathVariable String saleId) {
        // Auth required: only buyer/seller/admin should view; here we leave authorization to service or controller later
        UUID sId = UUID.fromString(saleId);
        List<Message> list = messageService.getMessagesBySale(sId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/messages/conversation")
    public ResponseEntity<?> getConversation(Authentication authentication, @RequestParam("with") String withUserId) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID me = UUID.fromString(authentication.getPrincipal().toString());
        UUID other = UUID.fromString(withUserId);
        List<Message> conv = messageService.getConversation(me, other);
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/messages/me")
    public ResponseEntity<?> myMessages(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        UUID me = UUID.fromString(authentication.getPrincipal().toString());
        List<Message> msgs = messageService.getUserMessages(me);
        return ResponseEntity.ok(msgs);
    }
}