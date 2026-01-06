package com.minewaku.chatter.adapter.mapper;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.web.response.ProfileDto;
import com.minewaku.chatter.adapter.web.response.UserDto;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Bio;
import com.minewaku.chatter.domain.value.DisplayName;
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
            URI.create(entity.getAvatar()),
            URI.create(entity.getCover()),
            new Username(entity.getUsername()),
            new DisplayName(entity.getDisplayName()),
            new Bio(entity.getDescription()),
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
    // MARK: THIS ONE FOR ADMIN, ADD ONE MORE FOR PROFILE 
    public UserDto entityToDto(JpaUserEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String email = entity.getEmail();
        String username = entity.getUsername();

        LocalDate birthday = entity.getBirthday();

        boolean enabled = Boolean.TRUE.equals(entity.getIsEnabled());
        boolean locked = Boolean.TRUE.equals(entity.getIsLocked());
        boolean deleted = Boolean.TRUE.equals(entity.getIsDeleted());

        Instant deletedAt = entity.getDeletedAt();
        Instant createdAt = entity.getCreatedAt();
        Instant modifiedAt = entity.getModifiedAt();

        return new UserDto(
            id,
            email,
            entity.getAvatar(),
            entity.getCover(),
            username,
            entity.getDisplayName(),
            entity.getDescription(),
            birthday,
            enabled,
            locked,
            deleted,
            deletedAt,
            createdAt,
            modifiedAt
        );
    }

    public ProfileDto domainToProfileDto(User domain) {
        if (domain == null) return null;
        ProfileDto dto = new ProfileDto(
            domain.getId() != null ? domain.getId().getValue() : -1L,
            domain.getUsername() != null ? domain.getUsername().getValue() : null,
            domain.getDisplayName() != null ? domain.getDisplayName().getValue() : null,
            domain.getBio() != null ? domain.getBio().getValue() : null,
            domain.getAvatar() != null ? domain.getAvatar().toString() : null,
            domain.getBanner() != null ? domain.getBanner().toString() : null,
            domain.getBirthday() != null ? domain.getBirthday().getValue() : null,
            domain.getAuditMetadata() != null ? domain.getAuditMetadata().getCreatedAt() : null
        );

        return dto;
    }

    public JpaUserEntity domainToEntity(User domain) {
        if (domain == null) return null;
        JpaUserEntity entity = new JpaUserEntity();
        entity.setId(domain.getId() != null ? domain.getId().getValue() : null);
        entity.setEmail(domain.getEmail() != null ? domain.getEmail().getValue() : null);
        entity.setAvatarKey(domain.getAvatarKey() != null ? domain.getAvatarKey() : null);
        entity.setAvatar(domain.getAvatar() != null ? domain.getAvatar().toString() : null);
        entity.setCoverKey(domain.getCoverKey() != null ? domain.getCoverKey() : null);
        entity.setCover(domain.getCover() != null ? domain.getCover().toString() : null);
        entity.setUsername(domain.getUsername() != null ? domain.getUsername().getValue() : null);
        entity.setDisplayName(domain.getDisplayName() != null ? domain.getDisplayName().getValue() : null);
        entity.setDescription(domain.getDescription() != null ? domain.getDescription().getValue() : null);
        entity.setBirthday(domain.getBirthday() != null ? domain.getBirthday().getValue() : null);
        entity.setIsEnabled(domain.isEnabled());
        entity.setIsLocked(domain.isLocked());
        entity.setIsDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getAuditMetadata().getCreatedAt());
        entity.setModifiedAt(domain.getAuditMetadata().getModifiedAt());
        return entity;
    }

    public Optional<User> entityToDomain(Optional<JpaUserEntity> entity) {
        return entity.map(this::entityToDomain);
    }
}
