package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity.JdbcUserEntity;

public interface JdbcUserRepository extends ListCrudRepository<JdbcUserEntity, Long> {
    Optional<JdbcUserEntity> findByEmail(String email);
}
