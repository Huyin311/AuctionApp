package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.dto.UpdateAvatarRequest;
import com.huyin.inner_auction.service.UserService;
import com.huyin.inner_auction.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for updating a user's avatar URL.
 *
 * NOTE: adjusted to use JwtUtil.getUserIdString(...) and JwtUtil.hasRole(...)
 */
@RestController
@RequestMapping("/api/users")
public class UserAvatarController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserAvatarController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PatchMapping("/{id}/avatar")
    public ResponseEntity<?> updateAvatar(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateAvatarRequest requestBody,
            HttpServletRequest request
    ) {
        // Extract token from Authorization header
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        String token = auth.substring(7);

        // Use JwtUtil.getUserIdString to obtain requester id as string
        String requesterUserId = jwtUtil.getUserIdString(token);
        boolean isAdmin = jwtUtil.hasRole(token, "ROLE_ADMIN") || jwtUtil.hasRole(token, "ADMIN");

        if (requesterUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_token"));
        }

        // Only allow the owner or an admin to change avatar
        if (!requesterUserId.equals(id) && !isAdmin) {
            throw new AccessDeniedException("Not allowed to change other user's avatar");
        }

        try {
            var updated = userService.updateAvatarUrl(id, requestBody.getAvatarUrl());
            return ResponseEntity.ok(Map.of("ok", true, "avatarUrl", updated.getAvatarUrl()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user_not_found"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "update_failed"));
        }
    }
}