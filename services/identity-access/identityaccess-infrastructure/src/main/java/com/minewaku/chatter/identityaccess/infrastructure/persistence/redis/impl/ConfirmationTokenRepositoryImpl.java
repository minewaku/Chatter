package com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.RedisConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.mapper.RedisConfirmationTokenMapper;

@Repository
public class ConfirmationTokenRepositoryImpl implements ConfirmationTokenRepository {

    private final RedisConfirmationTokenRepository redisConfirmationTokenRepository;
    private final RedisConfirmationTokenMapper confirmationTokenMapper;

    public ConfirmationTokenRepositoryImpl(
            RedisConfirmationTokenRepository redisConfirmationTokenRepository,
            RedisConfirmationTokenMapper confirmationTokenMapper) {
                
        this.redisConfirmationTokenRepository = redisConfirmationTokenRepository;
        this.confirmationTokenMapper = confirmationTokenMapper;
    }

    @Override
    public Optional<ConfirmationToken> findByToken(String token) {
        return confirmationTokenMapper.dtoToDomain(
                redisConfirmationTokenRepository.findByToken(token));
    }

    @Override
    public void deleteByEmail(Email email) {
        redisConfirmationTokenRepository.deleteByEmail(email.getValue());
    }

    @Override
    public ConfirmationToken save(ConfirmationToken confirmationToken) {
        return confirmationTokenMapper.dtoToDomain(
                redisConfirmationTokenRepository.save(
                        confirmationTokenMapper.domainToDto(confirmationToken)));
    }

}
