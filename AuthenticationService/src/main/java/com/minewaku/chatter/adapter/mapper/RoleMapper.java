package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

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

        AuditMetadata auditMetadata = new AuditMetadata(
            entity.getCreatedAt(),
            entity.getModifiedAt()
        );

        return Role.reconstitute(
            mapToRoleId(entity.getId()),
            entity.getName(),
            new Code(entity.getCode()), // Code string trong DB -> Value Object
            entity.getDescription(),
            auditMetadata,
            Boolean.TRUE.equals(entity.getDeleted()),
            entity.getDeletedAt()
        );
    }


    public JpaRoleEntity domainToEntity(Role domain) {
        if (domain == null) return null;

        return JpaRoleEntity.builder()
            .id(unwrapValue(domain.getId(), RoleId::getValue))
            .name(domain.getName())
            .code(unwrapValue(domain.getCode(), Code::getValue)) // Unwrap Code VO
            .description(domain.getDescription())
            .deleted(domain.isDeleted())
            .deletedAt(domain.getDeletedAt())
            .createdAt(domain.getAuditMetadata().getCreatedAt())
            .modifiedAt(domain.getAuditMetadata().getModifiedAt())
            .build();
    }


    public RoleDto entityToDto(JpaRoleEntity entity) {
        if (entity == null) return null;

        return new RoleDto(
            entity.getId() != null ? entity.getId() : -1L,
            entity.getName(),
            entity.getCode(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getModifiedAt(),
            Boolean.TRUE.equals(entity.getDeleted()),
            entity.getDeletedAt()
        );
    }


    public Optional<Role> entityToDomain(Optional<JpaRoleEntity> entity) {
        return entity.map(this::entityToDomain);
    }



    private RoleId mapToRoleId(Long id) {
        return new RoleId(id != null ? id : 0L);
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}