package com.huyin.inner_auction.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.*;

/**
 * JwtUtil helpers.
 *
 * Added:
 * - getUserIdString(token): returns user id as String (from "userId" claim or subject).
 * - hasRole(token, role): checks common claim names ("roles", "authorities") for presence of role.
 *
 * Kept the existing getUserId returning UUID for other callers.
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    private Key key;

    @PostConstruct
    private void init() {
        try {
            if (jwtSecret == null || jwtSecret.isBlank()) {
                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
                System.err.println("[WARN] No app.jwt.secret provided; using temporary key (do not use in production).");
            } else {
                byte[] bytes;
                try {
                    bytes = Base64.getDecoder().decode(jwtSecret);
                } catch (IllegalArgumentException ex) {
                    bytes = jwtSecret.getBytes();
                }
                this.key = Keys.hmacShaKeyFor(bytes);
            }
        } catch (WeakKeyException wke) {
            String msg = "Configured JWT secret is too weak. Provide a secret of at least 256 bits (32 bytes) or a base64 string representing >=32 bytes.";
            System.err.println("[FATAL] " + msg);
            throw new IllegalStateException(msg, wke);
        }
    }

    public String generateTokenForUserId(UUID userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Existing method returning UUID (may be null if subject/claim not a UUID).
     */
    public UUID getUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        Object userIdClaim = claims.get("userId");
        if (userIdClaim != null) {
            try {
                return UUID.fromString(userIdClaim.toString());
            } catch (IllegalArgumentException ignored) {}
        }
        String sub = claims.getSubject();
        if (sub == null) return null;
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * New helper: returns user id as String.
     * Prefers "userId" claim, falls back to subject. Returns null if neither present.
     */
    public String getUserIdString(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            Object userIdClaim = claims.get("userId");
            if (userIdClaim != null) return userIdClaim.toString();
            String sub = claims.getSubject();
            return (sub == null || sub.isBlank()) ? null : sub;
        } catch (JwtException ex) {
            return null;
        }
    }

    /**
     * New helper: check whether token contains the given role.
     *
     * Supports common claim names:
     * - "roles" : can be String (comma separated) or Collection
     * - "authorities" : Collection or String
     *
     * Role comparison is case-sensitive by default; caller may pass "ROLE_ADMIN" or "ADMIN".
     */
    public boolean hasRole(String token, String role) {
        if (token == null || role == null) return false;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

            // 1) check "roles" claim
            Object rolesClaim = claims.get("roles");
            if (rolesClaim != null) {
                if (rolesClaim instanceof Collection) {
                    for (Object o : (Collection<?>) rolesClaim) {
                        if (role.equals(String.valueOf(o)) || role.equalsIgnoreCase(String.valueOf(o))) return true;
                    }
                } else {
                    String s = String.valueOf(rolesClaim);
                    // comma separated
                    String[] parts = s.split("[,;\\s]+");
                    for (String p : parts) {
                        if (role.equals(p) || role.equalsIgnoreCase(p)) return true;
                    }
                }
            }

            // 2) check "authorities" claim
            Object authClaim = claims.get("authorities");
            if (authClaim != null) {
                if (authClaim instanceof Collection) {
                    for (Object o : (Collection<?>) authClaim) {
                        if (role.equals(String.valueOf(o)) || role.equalsIgnoreCase(String.valueOf(o))) return true;
                    }
                } else {
                    String s = String.valueOf(authClaim);
                    String[] parts = s.split("[,;\\s]+");
                    for (String p : parts) {
                        if (role.equals(p) || role.equalsIgnoreCase(p)) return true;
                    }
                }
            }

            // 3) check "scope" or "scopes" claim (space separated)
            Object scopeClaim = claims.get("scope");
            if (scopeClaim == null) scopeClaim = claims.get("scopes");
            if (scopeClaim != null) {
                String s = String.valueOf(scopeClaim);
                String[] parts = s.split("[,;\\s]+");
                for (String p : parts) {
                    if (role.equals(p) || role.equalsIgnoreCase(p)) return true;
                }
            }

            // 4) as last resort, check custom "roles" inside nested map (e.g., {"roles":["ROLE_ADMIN"]})
            // (not exhaustive; adjust to how your tokens are structured)
        } catch (JwtException ex) {
            // invalid token -> no role
        }
        return false;
    }

    // (thay thế method validate hiện tại bằng version log lỗi)
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            System.err.println("[JWT] Token expired: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            System.err.println("[JWT] Unsupported token: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            System.err.println("[JWT] Malformed token: " + ex.getMessage());
        } catch (SignatureException ex) {
            System.err.println("[JWT] Invalid signature: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.err.println("[JWT] Illegal argument: " + ex.getMessage());
        } catch (JwtException ex) {
            System.err.println("[JWT] JWT exception: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("[JWT] Unexpected exception while validating token: " + ex.getMessage());
        }
        return false;
    }
}