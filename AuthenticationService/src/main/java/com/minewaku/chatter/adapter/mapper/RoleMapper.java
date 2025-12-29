package com.minewaku.chatter.adapter.mapper;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaRoleEntity;
import com.minewaku.chatter.adapter.web.response.RoleDto;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Code;
import com.minewaku.chatter.domain.value.id.RoleId;

@Component
public class RoleMapper {
    public Role entityToDomain(JpaRoleEntity entity) {
        if (entity == null) return null;

        // Build audit metadata from BaseEntity fields (created/modified timestamps)
        AuditMetadata auditMetadata = new AuditMetadata(
            entity.getCreatedAt(),
            entity.getModifiedAt()
        );

        return Role.reconstitute(
            new RoleId(entity.getId() != null ? entity.getId().longValue() : 0L),
            entity.getName(),
            new Code(entity.getCode()),
            entity.getDescription(),
            auditMetadata,
            entity.getIsDeleted() != null && entity.getIsDeleted(),
            entity.getDeletedAt()
        );
    }

    public JpaRoleEntity domainToEntity(Role domain) {
        if (domain == null) return null;
        JpaRoleEntity entity = new JpaRoleEntity();
        entity.setId(domain.getId() != null ? domain.getId().getValue() : null);
        entity.setName(domain.getName());
        entity.setCode(domain.getCode() != null ? domain.getCode().getValue() : null);
        entity.setDescription(domain.getDescription());
        entity.setIsDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getAuditMetadata().getCreatedAt());
        entity.setModifiedAt(domain.getAuditMetadata().getModifiedAt());
        return entity;
    }

    public Optional<Role> entityToDomain(Optional<JpaRoleEntity> entity) {
        return entity.map(this::entityToDomain);
    }

    /**
     * Map JPA entity to RoleDTO for presentation layers.
     */
    public RoleDto entityToDto(JpaRoleEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String name = entity.getName();
        String code = entity.getCode();
        String description = entity.getDescription();

        Instant createdAt = entity.getCreatedAt();
        Instant modifiedAt = entity.getModifiedAt();

        Boolean isDeleted = entity.getIsDeleted() != null && entity.getIsDeleted();
        Instant deletedAt = entity.getDeletedAt();

        return new RoleDto(id, name, code, description, createdAt, modifiedAt, isDeleted, deletedAt);
    }
}
