package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.redis.dto.RefreshTokenDto;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.value.id.OpaqueToken;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class RefreshTokenMapper {

    public RefreshTokenDto domainToDto(RefreshToken domain) {
        if (domain == null)
            return null;

        return new RefreshTokenDto(
                domain.getToken().getValue(),
                domain.getDuration(),
                domain.getIssuedAt(),
                domain.getExpiresAt(),
                domain.getUserId() != null ? domain.getUserId().getValue() : null,
                domain.getReplacedBy() != null ? domain.getReplacedBy().getValue() : null,
                domain.isRevoked(),
                domain.getRevokedAt());
    }

    public RefreshToken dtoToDomain(RefreshTokenDto dto) {
        if (dto == null)
            return null;

        return RefreshToken.reconstitute(
                new OpaqueToken(dto.token()),
                dto.duration(),
                dto.issuedAt(),
                dto.expiresAt(),
                new UserId(dto.userId()),
                dto.replacedBy() != null ? new OpaqueToken(dto.replacedBy()) : null,
                dto.revoked() != null ? dto.revoked() : false,
                dto.revokedAt());
    }

    public Optional<RefreshTokenDto> domainToDto(Optional<RefreshToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<RefreshToken> dtoToDomain(Optional<RefreshTokenDto> dto) {
        return dto.map(this::dtoToDomain);
    }
}