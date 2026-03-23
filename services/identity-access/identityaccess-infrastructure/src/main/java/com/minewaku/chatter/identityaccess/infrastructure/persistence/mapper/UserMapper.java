package com.minewaku.chatter.identityaccess.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.infrastructure.entity.JpaUserEntity;

@Component
public class UserMapper {
    
    public JpaUserEntity domainToEntity(User user) {
        return null;
    }

    public User entityToDomain(JpaUserEntity jpaUserEntity) {
        return null;
    }
}
