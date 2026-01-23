package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

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

        UserId userId = mapToUserId(entity.getId());
        AuditMetadata auditMetadata = new AuditMetadata(entity.getCreatedAt(), entity.getModifiedAt());

        return User.reconstitute(
            userId,
            new Email(entity.getEmail()),
            new Username(entity.getUsername()),
            new Birthday(entity.getBirthday()),
            auditMetadata,
            Boolean.TRUE.equals(entity.getEnabled()),
            Boolean.TRUE.equals(entity.getLocked()),
            Boolean.TRUE.equals(entity.getDeleted()),
            entity.getDeletedAt()
        );
    }


    public UserDto entityToDto(JpaUserEntity entity) {
        if (entity == null) return null;

        return new UserDto(
            entity.getId() != null ? entity.getId() : -1L,
            entity.getEmail(),
            entity.getUsername(),
            entity.getBirthday(),
            Boolean.TRUE.equals(entity.getEnabled()),
            Boolean.TRUE.equals(entity.getLocked()),
            Boolean.TRUE.equals(entity.getDeleted()),
            entity.getDeletedAt(),
            entity.getCreatedAt(),
            entity.getModifiedAt()
        );
    }


    public JpaUserEntity domainToEntity(User domain) {
        if (domain == null) return null;

        return JpaUserEntity.builder()
            .id(unwrapValue(domain.getId(), UserId::getValue))
            .email(unwrapValue(domain.getEmail(), Email::getValue))
            .username(unwrapValue(domain.getUsername(), Username::getValue))
            .birthday(unwrapValue(domain.getBirthday(), Birthday::getValue))
            .enabled(domain.isEnabled())
            .locked(domain.isLocked())
            .deleted(domain.isDeleted())
            .deletedAt(domain.getDeletedAt())
            .createdAt(domain.getAuditMetadata().getCreatedAt())
            .modifiedAt(domain.getAuditMetadata().getModifiedAt())
            .build();
    }


    
    public Optional<User> entityToDomain(Optional<JpaUserEntity> entity) {
        return entity.map(this::entityToDomain);
    }

    private UserId mapToUserId(Long id) {
        return id != null ? new UserId(id) : null;
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}
