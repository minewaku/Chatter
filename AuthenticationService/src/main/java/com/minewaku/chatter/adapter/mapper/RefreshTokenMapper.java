package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.redis.dto.RefreshTokenDto;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.value.id.OpaqueToken;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class RefreshTokenMapper {

    public RefreshTokenDto domainToDto(RefreshToken domain) {
        if (domain == null) return null;

        return new RefreshTokenDto(
            unwrapValue(domain.getToken(), OpaqueToken::getValue),
            domain.getDuration(),
            domain.getIssuedAt(),
            domain.getExpiresAt(),
            unwrapValue(domain.getUserId(), UserId::getValue),
            unwrapValue(domain.getReplacedBy(), OpaqueToken::getValue),
            domain.isRevoked(),
            domain.getRevokedAt()
        );
    }


    public RefreshToken dtoToDomain(RefreshTokenDto dto) {
        if (dto == null) return null;

        return RefreshToken.reconstitute(
            mapToOpaqueToken(dto.token()),
            dto.duration(),
            dto.issuedAt(),
            dto.expiresAt(),
            mapToUserId(dto.userId()),
            mapToOpaqueToken(dto.replacedBy()),
            Boolean.TRUE.equals(dto.revoked()),
            dto.revokedAt()
        );
    }


    public Optional<RefreshTokenDto> domainToDto(Optional<RefreshToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<RefreshToken> dtoToDomain(Optional<RefreshTokenDto> dto) {
        return dto.map(this::dtoToDomain);
    }



    private OpaqueToken mapToOpaqueToken(String token) {
        return token != null ? new OpaqueToken(token) : null;
    }

    private UserId mapToUserId(Long userId) {
        return userId != null ? new UserId(userId) : null;
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}