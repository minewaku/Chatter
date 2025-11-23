package com.minewaku.chatter.adapter.mapper;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.web.response.UserDTO;
import com.minewaku.chatter.domain.model.User;
import java.time.Instant;
import java.time.LocalDate;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.id.UserId;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User entityToDomain(JpaUserEntity entity) {
        if (entity == null) return null;
        AuditMetadata auditMetadata = new AuditMetadata(
            entity.getCreatedDate(),
            entity.getModifiedDate()
        );
        UserId userId = entity.getId() != null ? new UserId(entity.getId().longValue()) : null;
        User domain = User.reconstitute(
            userId,
            new Email(entity.getEmail()),
            new Username(entity.getUsername()),
            new Birthday(entity.getBirthday()),
            auditMetadata,
            entity.getIsEnabled(),
            entity.getIsLocked(),
            entity.getIsDeleted(),
            entity.getDeletedAt()
        );
        domain.setEnabled(Boolean.TRUE.equals(entity.getIsEnabled()));
        domain.setLocked(Boolean.TRUE.equals(entity.getIsLocked()));
        domain.setDeleted(Boolean.TRUE.equals(entity.getIsDeleted()));
        return domain;
    }

    /**
     * Map JPA entity directly to a scalar-only UserDTO. This avoids exposing
     * domain value objects to presentation layers.
     */
    public UserDTO entityToDto(JpaUserEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String email = entity.getEmail();
        String username = entity.getUsername();

        LocalDate birthday = entity.getBirthday();

        boolean enabled = Boolean.TRUE.equals(entity.getIsEnabled());
        boolean locked = Boolean.TRUE.equals(entity.getIsLocked());
        boolean deleted = Boolean.TRUE.equals(entity.getIsDeleted());

        Instant deletedAt = entity.getDeletedAt();
        Instant createdAt = entity.getCreatedDate();
        Instant modifiedAt = entity.getModifiedDate();

        return new UserDTO(
            id,
            email,
            username,
            birthday,
            enabled,
            locked,
            deleted,
            deletedAt,
            createdAt,
            modifiedAt
        );
    }

    public JpaUserEntity domainToEntity(User domain) {
        if (domain == null) return null;
        JpaUserEntity entity = new JpaUserEntity();
        entity.setId(domain.getId() != null ? domain.getId().getValue() : null);
        entity.setEmail(domain.getEmail() != null ? domain.getEmail().getValue() : null);
        entity.setUsername(domain.getUsername() != null ? domain.getUsername().getValue() : null);
        entity.setBirthday(domain.getBirthday() != null ? domain.getBirthday().getValue() : null);
        entity.setIsEnabled(domain.isEnabled());
        entity.setIsLocked(domain.isLocked());
        entity.setIsDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedDate(domain.getAuditMetadata().getCreatedAt());
        entity.setModifiedDate(domain.getAuditMetadata().getModifiedAt());
        return entity;
    }

    public Optional<User> entityToDomain(Optional<JpaUserEntity> entity) {
        return entity.map(this::entityToDomain);
    }
}
