package com.huyin.inner_auction.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

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

    public UUID getUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        Object userIdClaim = claims.get("userId");
        if (userIdClaim != null) {
            return UUID.fromString(userIdClaim.toString());
        }
        String sub = claims.getSubject();
        if (sub == null) return null;
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}