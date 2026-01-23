package com.minewaku.chatter.adapter.db.redis.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.redis.RedisRefreshTokenRepository;
import com.minewaku.chatter.adapter.mapper.RefreshTokenMapper;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;
import com.minewaku.chatter.domain.value.id.OpaqueToken;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    @Autowired
    private RedisRefreshTokenRepository redisRefreshTokenRepository;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Override
    public void save(RefreshToken refreshToken) {
        redisRefreshTokenRepository.save(refreshTokenMapper.domainToDto(refreshToken));
    }

    @Override
    public void update(RefreshToken refreshToken) {
        redisRefreshTokenRepository.update(refreshTokenMapper.domainToDto(refreshToken));
    }

    @Override
    public void revoke(RefreshToken refreshToken) {
        redisRefreshTokenRepository.revoke(refreshTokenMapper.domainToDto(refreshToken));
    }
    
    @Override
    public void deleteByToken(OpaqueToken token) {
        redisRefreshTokenRepository.deleteByToken(token.getValue());
    }

    @Override
    public Optional<RefreshToken> findByToken(OpaqueToken token) {
        return refreshTokenMapper.dtoToDomain(redisRefreshTokenRepository.findByToken(token.getValue()));
    }

}
