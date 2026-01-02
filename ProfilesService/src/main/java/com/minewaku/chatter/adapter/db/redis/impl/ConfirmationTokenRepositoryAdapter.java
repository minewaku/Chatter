package com.minewaku.chatter.adapter.db.redis.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.RedisConfirmationTokenRepository;
import com.minewaku.chatter.adapter.mapper.ConfirmationTokenMapper;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.value.Email;

@Repository
public class ConfirmationTokenRepositoryAdapter implements ConfirmationTokenRepository {

    @Autowired
    private RedisConfirmationTokenRepository redisConfirmationTokenRepository;

    @Autowired
    private ConfirmationTokenMapper confirmationTokenMapper;

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
