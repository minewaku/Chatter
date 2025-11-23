package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.entity.JpaUserRoleEntity;
import com.minewaku.chatter.adapter.entity.embededKey.JpaUserRoleId;
import com.minewaku.chatter.domain.model.UserRole;
import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.id.UserId;
import com.minewaku.chatter.domain.value.id.UserRoleId;

@Component
public class UserRoleMapper {
    public UserRole entityToDomain(JpaUserRoleEntity entity) {
        if (entity == null || entity.getId() == null) return null;

        Long userIdInt = entity.getId().getUserId() != null ? entity.getId().getUserId() : null;
        Long roleIdInt = entity.getId().getRoleId() != null ? entity.getId().getRoleId() : null;

        UserId userId = userIdInt != null ? new UserId(userIdInt.longValue()) : null;
        RoleId roleId = roleIdInt != null ? new RoleId(roleIdInt.longValue()) : null;

        UserRoleId userRoleId = (userId != null && roleId != null)
            ? new UserRoleId(userId, roleId)
            : null;

        UserId createdBy = entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null;
        return UserRole.reconstitute(userRoleId, createdBy);
    }

    public JpaUserRoleEntity domainToEntity(UserRole domain) {
        if (domain == null) return null;

        JpaUserRoleEntity entity = new JpaUserRoleEntity();

        // ID (composite key)
        if (domain.getUserRoleId() != null
                && domain.getUserRoleId().getUserId() != null
                && domain.getUserRoleId().getRoleId() != null) {
            long userId = domain.getUserRoleId().getUserId().getValue();
            long roleId = domain.getUserRoleId().getRoleId().getValue();
            entity.setId(new JpaUserRoleId(userId, roleId));
        }

        if (domain.getCreatedBy() != null) {
            entity.setCreatedBy(domain.getCreatedBy().getValue());
        }
        // propagate deletion flag from domain model when available
        // default false to avoid nulls
        return entity;
    }

    public Optional<UserRole> entityToDomain(Optional<JpaUserRoleEntity> entity) {
        return entity.map(this::entityToDomain);
    }
}

