package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

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

        return UserRole.reconstitute(
            mapToDomainId(entity.getId()),
            mapToUserId(entity.getCreatedBy())
        );
    }


    public JpaUserRoleEntity domainToEntity(UserRole domain) {
        if (domain == null) return null;

        return JpaUserRoleEntity.builder()
            .id(mapToJpaId(domain.getUserRoleId()))
            .createdBy(unwrapValue(domain.getCreatedBy(), UserId::getValue))
            .build();
    }


    public Optional<UserRole> entityToDomain(Optional<JpaUserRoleEntity> entity) {
        return entity.map(this::entityToDomain);
    }



    private UserRoleId mapToDomainId(JpaUserRoleId jpaId) {
        if (jpaId == null) return null;
        
        UserId userId = mapToUserId(jpaId.getUserId());
        RoleId roleId = mapToRoleId(jpaId.getRoleId());

        if (userId != null && roleId != null) {
            return new UserRoleId(userId, roleId);
        }
        return null;
    }

    private JpaUserRoleId mapToJpaId(UserRoleId domainId) {
        if (domainId == null) return null;
        
        Long userId = unwrapValue(domainId.getUserId(), UserId::getValue);
        Long roleId = unwrapValue(domainId.getRoleId(), RoleId::getValue);

        if (userId != null && roleId != null) {
            return new JpaUserRoleId(userId, roleId);
        }
        return null;
    }

    private UserId mapToUserId(Long id) {
        return id != null ? new UserId(id) : null;
    }

    private RoleId mapToRoleId(Long id) {
        return id != null ? new RoleId(id) : null;
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}