package com.huyin.inner_auction.controller;

import com.huyin.inner_auction.dto.UserDto;
import com.huyin.inner_auction.dto.UpdateUserRequest;
import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") UUID id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        // Map only public fields required by frontend
        UserDto dto = new UserDto(
                u.getId(),
                u.getDisplayName(), // ensure your User entity has this field or change accordingly
                u.getAvatarUrl(),   // ensure the field exists
                u.getEmail(),
                u.getBio()          // optional
        );
        return ResponseEntity.ok(dto);
    }

    /**
     * Update user fields (displayName, bio, avatarUrl).
     * Only the owner (same user id) or ROLE_ADMIN allowed.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUserById(
            @PathVariable("id") UUID id,
            @RequestBody UpdateUserRequest req,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // principal in your app is the userId string (as used in WalletController). Adjust if different.
        String principal = authentication.getPrincipal() == null ? "" : authentication.getPrincipal().toString();

        boolean isOwner;
        try {
            isOwner = principal.equalsIgnoreCase(id.toString());
        } catch (Exception ex) {
            isOwner = false;
        }

        boolean isAdmin = authentication.getAuthorities() != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        // apply updates only if fields are non-null (allow empty string if you want to clear)
        if (req.getDisplayName() != null) {
            String dn = req.getDisplayName().trim();
            if (dn.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            user.setDisplayName(dn);
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }

        User saved = userRepository.save(user);

        UserDto dto = new UserDto(
                saved.getId(),
                saved.getDisplayName(),
                saved.getAvatarUrl(),
                saved.getEmail(),
                saved.getBio()
        );
        return ResponseEntity.ok(dto);
    }
}