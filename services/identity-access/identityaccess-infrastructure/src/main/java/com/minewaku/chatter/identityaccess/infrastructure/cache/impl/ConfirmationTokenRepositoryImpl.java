package com.minewaku.chatter.identityaccess.infrastructure.cache.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.infrastructure.cache.RedisConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.infrastructure.cache.mapper.ConfirmationTokenMapper;

@Repository
public class ConfirmationTokenRepositoryImpl implements ConfirmationTokenRepository {

    private final RedisConfirmationTokenRepository redisConfirmationTokenRepository;
    private final ConfirmationTokenMapper confirmationTokenMapper;

    public ConfirmationTokenRepositoryImpl(
            RedisConfirmationTokenRepository redisConfirmationTokenRepository,
            ConfirmationTokenMapper confirmationTokenMapper) {
                
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
    public void confirmEmail(ConfirmationToken token) {
        token.verifyToken();
        save(token);
    }

    @Override
    public void deleteByToken(String token) {
        redisConfirmationTokenRepository.deleteByToken(token);
    }

    @Override
    public ConfirmationToken save(ConfirmationToken confirmationToken) {
        return confirmationTokenMapper.dtoToDomain(
                redisConfirmationTokenRepository.save(
                        confirmationTokenMapper.domainToDto(confirmationToken)));
    }

}
