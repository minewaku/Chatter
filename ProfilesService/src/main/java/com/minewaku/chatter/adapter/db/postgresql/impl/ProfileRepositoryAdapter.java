package com.minewaku.chatter.adapter.db.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.postgresql.JpaProfileRepository;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.repository.ProfileRepository;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

@Repository
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final JpaProfileRepository jpaProfileRepository;
    private final UserMapper userMapper;

    public ProfileRepositoryAdapter(
            JpaProfileRepository jpaProfileRepository,
            UserMapper userMapper) {

        this.jpaProfileRepository = jpaProfileRepository;
        this.userMapper = userMapper;
    }


    public Optional<User> findActivatedByUsername(Username username) {
        return userMapper.entityToDomain(
                jpaProfileRepository.findActivatedByUsername(username.getValue()));
    }
    
    public Optional<User> findActivatedByUserId(UserId userId) {
        return userMapper.entityToDomain(
                jpaProfileRepository.findActivatedByUserId(userId.getValue()));
    }

    public void updateUser(User user) {
        jpaProfileRepository.save(userMapper.domainToEntity(user));
    }

    public void uploadAvatarUrl(User user) {
        jpaProfileRepository.uploadAvatar(userMapper.domainToEntity(user));
    }

    public void uploadBannerUrl(User user) {
        jpaProfileRepository.uploadBanner(userMapper.domainToEntity(user));
    }
}