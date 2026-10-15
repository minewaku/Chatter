package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account")
record UserAccountRow(
        @Id Long id,
        String normalizedEmail,
        String username,
        LocalDate birthday,
        String status,
        OffsetDateTime deletedAt,
        String passwordAlgorithm,
        String passwordHash,
        byte[] passwordSalt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @Version Long version) {}
