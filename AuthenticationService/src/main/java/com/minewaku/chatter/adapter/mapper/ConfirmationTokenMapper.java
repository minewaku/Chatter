package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.redis.dto.ConfirmationTokenDto;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class ConfirmationTokenMapper {

    public ConfirmationTokenDto domainToDto(ConfirmationToken domain) {
        if (domain == null)
            return null;

        return new ConfirmationTokenDto(
                domain.getToken(),
                domain.getUserId() != null ? domain.getUserId().getValue() : null,
                domain.getEmail() != null ? domain.getEmail().getValue() : null,
                domain.getDuration(),
                domain.getCreatedAt(),
                domain.getExpiresAt(),
                domain.getConfirmedAt());
    }

    public ConfirmationToken dtoToDomain(ConfirmationTokenDto dto) {
        if (dto == null)
            return null;

        return ConfirmationToken.reconstitute(
                dto.token(),
                new UserId(dto.userId()),
                new Email(dto.email()),
                dto.duration(),
                dto.createdAt(),
                dto.expiresAt(),
                dto.confirmedAt());
    }

    public Optional<ConfirmationTokenDto> domainToDto(Optional<ConfirmationToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<ConfirmationToken> dtoToDomain(Optional<ConfirmationTokenDto> dto) {
        return dto.map(this::dtoToDomain);
    }
}
