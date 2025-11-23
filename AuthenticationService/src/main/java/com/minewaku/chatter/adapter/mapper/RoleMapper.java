package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaRoleEntity;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.value.Code;
import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.adapter.web.response.RoleDTO;
import java.time.Instant;

@Component
public class RoleMapper {
    public Role entityToDomain(JpaRoleEntity entity) {
        if (entity == null) return null;

        // Build audit metadata from BaseEntity fields (created/modified timestamps)
        AuditMetadata auditMetadata = new AuditMetadata(
            entity.getCreatedDate(),
            entity.getModifiedDate()
        );

        return Role.reconstitute(
            new RoleId(entity.getId() != null ? entity.getId().longValue() : 0L),
            entity.getName(),
            new Code(entity.getCode()),
            entity.getDescription(),
            auditMetadata,
            entity.getIsDeleted() != null && entity.getIsDeleted()
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
        return entity;
    }

    public Optional<Role> entityToDomain(Optional<JpaRoleEntity> entity) {
        return entity.map(this::entityToDomain);
    }

    /**
     * Map JPA entity to RoleDTO for presentation layers.
     */
    public RoleDTO entityToDto(JpaRoleEntity entity) {
        if (entity == null) return null;

        long id = entity.getId() != null ? entity.getId().longValue() : -1L;
        String name = entity.getName();
        String code = entity.getCode();
        String description = entity.getDescription();

        // JpaRoleEntity does not track deletedAt timestamp; set to null
        Instant deletedAt = null;
        Instant createdAt = entity.getCreatedDate();
        Instant modifiedAt = entity.getModifiedDate();

        return new RoleDTO(id, name, code, description, deletedAt, createdAt, modifiedAt);
    }
}
