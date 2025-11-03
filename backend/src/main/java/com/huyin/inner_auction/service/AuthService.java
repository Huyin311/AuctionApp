package com.huyin.inner_auction.service;

import java.util.UUID;

/**
 * Service API for authentication operations.
 * Implementations should handle registration and login logic.
 */
public interface AuthService {
    /**
     * Register a new user.
     * Returns a JWT access token for convenience (dev).
     *
     * @param email raw email
     * @param rawPassword raw password
     * @param role optional role (BUYER/SELLER). If null, default to BUYER.
     * @return JWT access token string
     */
    String register(String email, String rawPassword, String role);

    /**
     * Authenticate a user and return JWT access token.
     *
     * @param email raw email
     * @param rawPassword raw password
     * @return JWT access token string
     */
    String login(String email, String rawPassword);

    /**
     * Resolve user id from email (helper).
     *
     * @param email user email
     * @return user id
     */
    UUID findUserIdByEmail(String email);
}