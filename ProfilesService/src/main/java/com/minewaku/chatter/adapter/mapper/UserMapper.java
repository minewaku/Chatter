package com.minewaku.chatter.adapter.mapper;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.messaging.message.CreateUserMessage;
import com.minewaku.chatter.adapter.web.response.ProfileDto;
import com.minewaku.chatter.adapter.web.response.UserDto;
import com.minewaku.chatter.domain.command.profile.CreateUserCommand;
import com.minewaku.chatter.domain.model.StorageFile;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Bio;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.DisplayName;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.StorageKey;
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

        User domain;
        try {
            URI avatarUri = new URI(entity.getAvatar());
            URI coverUri = new URI(entity.getCover());

            domain = User.reconstitute(
                userId,
                new Email(entity.getEmail()),
                new StorageFile(new StorageKey(entity.getAvatarKey()), avatarUri),
                new StorageFile(new StorageKey(entity.getCoverKey()), coverUri),
                new Username(entity.getUsername()),
                new DisplayName(entity.getDisplayName()),
                new Bio(entity.getBio()),
                new Birthday(entity.getBirthday()),
                auditMetadata,
                entity.getDiscoverable(),
                entity.getEnabled(),
                entity.getLocked(),
                entity.getDeleted(),
                entity.getDeletedAt()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URI in entity", e);
        }

        return domain;
    }

    // MARK: THIS ONE FOR ADMIN, ADD ONE MORE FOR PROFILE 
    public UserDto entityToDto(JpaUserEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String email = entity.getEmail();
        String username = entity.getUsername();

        LocalDate birthday = entity.getBirthday();

        boolean discoverable = Boolean.TRUE.equals(entity.getDiscoverable());
        boolean enabled = Boolean.TRUE.equals(entity.getEnabled());
        boolean locked = Boolean.TRUE.equals(entity.getLocked());
        boolean deleted = Boolean.TRUE.equals(entity.getDeleted());

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
            entity.getBio(),
            birthday,
            discoverable,
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
        entity.setAvatarKey(domain.getAvatar().getKey() != null ? domain.getAvatar().getKey().getValue() : null);
        entity.setAvatar(domain.getAvatar().getUri() != null ? domain.getAvatar().toString() : null);
        entity.setCoverKey(domain.getBanner().getKey() != null ? domain.getBanner().getKey().getValue() : null);
        entity.setCover(domain.getBanner().getUri() != null ? domain.getBanner().getUri().getPath() : null);
        entity.setUsername(domain.getUsername() != null ? domain.getUsername().getValue() : null);
        entity.setDisplayName(domain.getDisplayName() != null ? domain.getDisplayName().getValue() : null);
        entity.setBio(domain.getBio() != null ? domain.getBio().getValue() : null);
        entity.setBirthday(domain.getBirthday() != null ? domain.getBirthday().getValue() : null);
        entity.setEnabled(domain.isEnabled());
        entity.setLocked(domain.isLocked());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getAuditMetadata().getCreatedAt());
        entity.setModifiedAt(domain.getAuditMetadata().getModifiedAt());
        return entity;
    }

    public Optional<User> entityToDomain(Optional<JpaUserEntity> entity) {
        return entity.map(this::entityToDomain);
    }

    public CreateUserCommand CreateUserMessageToCommand(CreateUserMessage message) {
        return new CreateUserCommand(
                new UserId(message.id()),
                new Email(message.email()),
                new Username(message.username()),
                new Birthday(LocalDate.parse(message.birthday())),
                message.enabled(),
                message.locked(),
                message.deleted(),
                Instant.parse(message.deletedAt()),
                new AuditMetadata(
                    Instant.parse(message.createdAt()),
                    Instant.parse(message.modifiedAt())
            )
        );
    }
}
