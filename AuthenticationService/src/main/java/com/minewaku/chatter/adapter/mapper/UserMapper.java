package com.minewaku.chatter.adapter.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.web.response.UserDto;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class UserMapper {
    public User entityToDomain(JpaUserEntity entity) {
        if (entity == null) return null;
        AuditMetadata auditMetadata = new AuditMetadata(
            entity.getCreatedAt(),
            entity.getModifiedAt()
        );
        UserId userId = entity.getId() != null ? new UserId(entity.getId().longValue()) : null;

        User domain = User.reconstitute(
            userId,
            new Email(entity.getEmail()),
            new Username(entity.getUsername()),
            new Birthday(entity.getBirthday()),
            auditMetadata,
            entity.getEnabled(),
            entity.getLocked(),
            entity.getDeleted(),
            entity.getDeletedAt()
        );

        return domain;
    }

    public UserDto entityToDto(JpaUserEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String email = entity.getEmail();
        String username = entity.getUsername();

        LocalDate birthday = entity.getBirthday();

        boolean enabled = Boolean.TRUE.equals(entity.getEnabled());
        boolean locked = Boolean.TRUE.equals(entity.getLocked());
        boolean deleted = Boolean.TRUE.equals(entity.getDeleted());

        Instant deletedAt = entity.getDeletedAt();
        Instant createdAt = entity.getCreatedAt();
        Instant modifiedAt = entity.getModifiedAt();

        return new UserDto(
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
        entity.setEnabled(domain.isEnabled());
        entity.setLocked(domain.isLocked());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt() != null ? domain.getDeletedAt() : null);
        entity.setCreatedAt(domain.getAuditMetadata().getCreatedAt());
        entity.setModifiedAt(domain.getAuditMetadata().getModifiedAt());
        return entity;
    }

    public Optional<User> entityToDomain(Optional<JpaUserEntity> entity) {
        return entity.map(this::entityToDomain);
    }
}
