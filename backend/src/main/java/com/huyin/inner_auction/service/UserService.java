package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.User;
import com.huyin.inner_auction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Update only avatarUrl field of user.
     * Throws IllegalArgumentException if user not found.
     */
    @Transactional
    public User updateAvatarUrl(String userIdStr, String avatarUrl) {
        UUID userId = UUID.fromString(userIdStr);
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userIdStr);
        }
        User user = opt.get();
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    // Other user-related methods can be added here
}