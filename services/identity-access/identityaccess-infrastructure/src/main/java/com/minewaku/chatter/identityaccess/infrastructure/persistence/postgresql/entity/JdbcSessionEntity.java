package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto.JdbcDeviceInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("session")
public class JdbcSessionEntity {

    @Id 
    @Column("session_id")
    private UUID id;

    @Column("user_id")
    private Long userId;

    @Column("device_info")
    private JdbcDeviceInfo deviceInfo;

    @Column("generation")
    private Integer generation;

    @Column("issued_at")
    private Instant issuedAt;

    @Column("last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("is_revoked")
    private Boolean revoked;

    @Column("revoked_at")
    private Instant revokedAt;

    @Version
    private Integer version;
}