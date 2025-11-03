package com.huyin.inner_auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // BUYER | SELLER | ADMIN

    private String displayName;
    private String phone;

    @Column(nullable = false)
    private Boolean depositPaid = false;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.valueOf(0.00);

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}