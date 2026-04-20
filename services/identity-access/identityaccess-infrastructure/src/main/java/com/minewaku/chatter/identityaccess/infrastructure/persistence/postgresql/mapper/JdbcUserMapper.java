package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Birthday;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Enablement;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Username;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Credentials;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.AuditMetadata;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.DeletionStatus;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcUserEntity;

@Component
public class JdbcUserMapper {

    public JdbcUserEntity domainToEntity(User domain) {
        if (domain == null) {
            return null;
        }

        return JdbcUserEntity.builder()
            // Đã đổi sang kiểu Long, không cần parse UUID nữa
            .id(domain.getId() != null ? domain.getId().getValue() : null)
            
            .email(domain.getEmail() != null ? domain.getEmail().getValue() : null)
            .username(domain.getUsername() != null ? domain.getUsername().getValue() : null)
            .birthday(domain.getBirthday() != null ? domain.getBirthday().getValue() : null)
            
            // Mapping Enablement
            .enabled(domain.getEnablement() != null && domain.getEnablement().isEnabled())
            .locked(domain.getEnablement() != null && domain.getEnablement().isLocked())
            .deleted(domain.getEnablement() != null && domain.getEnablement().getDeletionStatus() != null && domain.getEnablement().getDeletionStatus().isDeleted())
            .deletedAt(domain.getEnablement() != null && domain.getEnablement().getDeletionStatus() != null ? domain.getEnablement().getDeletionStatus().getDeletedAt() : null)
            
            // Mapping Credentials
            .hashedPassword(domain.getCredentials() != null && domain.getCredentials().getHashedPassword() != null ? domain.getCredentials().getHashedPassword().getHash() : null)
            .algorithm(domain.getCredentials() != null && domain.getCredentials().getHashedPassword() != null ? domain.getCredentials().getHashedPassword().getAlgorithm() : null)
            .salt(domain.getCredentials() != null && domain.getCredentials().getHashedPassword() != null ? domain.getCredentials().getHashedPassword().getSalt() : null)
            .passwordModifiedAt(domain.getCredentials() != null ? domain.getCredentials().getModifiedAt() : null)
            
            // Mapping AuditMetadata
            .createdAt(domain.getAuditMetadata() != null ? domain.getAuditMetadata().getCreatedAt() : null)
            .modifiedAt(domain.getAuditMetadata() != null ? domain.getAuditMetadata().getModifiedAt() : null)
            .version(domain.getVersion())
            .build();
    }

    public User entityToDomain(JdbcUserEntity entity) {
        if (entity == null) {
            return null;
        }

        // Truyền trực tiếp giá trị Long (entity.getId()) vào constructor của UserId
        UserId userId = entity.getId() != null ? new UserId(entity.getId()) : null;
        
        Email email = entity.getEmail() != null ? new Email(entity.getEmail()) : null;
        Username username = entity.getUsername() != null ? new Username(entity.getUsername()) : null;
        Birthday birthday = entity.getBirthday() != null ? new Birthday(entity.getBirthday()) : null;

        return User.reconstitute(
            userId,
            email,
            username,
            birthday,
            mapEnablement(entity),
            mapAuditMetadata(entity),
            mapCredentials(entity),
            entity.getVersion()
        );
    }

    public Optional<User> entityToDomain(Optional<JdbcUserEntity> entityOptional) {
        return entityOptional.map(this::entityToDomain);
    }

    // --- Methods for List Mapping ---

    public List<JdbcUserEntity> domainListToEntityList(List<User> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        return domains.stream()
            .map(this::domainToEntity)
            .toList();
    }

    public List<User> entityListToDomainList(List<JdbcUserEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
            .map(this::entityToDomain)
            .toList();
    }

    // --- Private Helper Methods ---

    private Enablement mapEnablement(JdbcUserEntity entity) {
        DeletionStatus deletionStatus = new DeletionStatus(
            entity.getDeleted() != null ? entity.getDeleted() : false,
            entity.getDeletedAt()
        );

        return new Enablement(
            entity.getEnabled() != null ? entity.getEnabled() : false,
            entity.getLocked() != null ? entity.getLocked() : false,
            deletionStatus
        );
    }

    private Credentials mapCredentials(JdbcUserEntity entity) {
        HashedPassword hashedPassword = new HashedPassword(
            entity.getAlgorithm(),
            entity.getHashedPassword(),
            entity.getSalt()
        );

        return Credentials.reconstitute(
            hashedPassword,
            entity.getPasswordModifiedAt()
        );
    }

    private AuditMetadata mapAuditMetadata(JdbcUserEntity entity) {
        Instant createdAt = entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now();
        Instant modifiedAt = entity.getModifiedAt() != null ? entity.getModifiedAt() : Instant.now();
        
        return new AuditMetadata(createdAt, modifiedAt);
    }
}