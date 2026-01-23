package com.minewaku.chatter.adapter.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.redis.dto.ConfirmationTokenDto;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class ConfirmationTokenMapper {

    public ConfirmationTokenDto domainToDto(ConfirmationToken domain) {
        if (domain == null) return null;

        return new ConfirmationTokenDto(
            domain.getToken(),
            unwrapValue(domain.getUserId(), UserId::getValue),
            unwrapValue(domain.getEmail(), Email::getValue),
            domain.getDuration(),
            domain.getCreatedAt(),
            domain.getExpiresAt(),
            domain.getConfirmedAt()
        );
    }

    public ConfirmationToken dtoToDomain(ConfirmationTokenDto dto) {
        if (dto == null) return null;

        return ConfirmationToken.reconstitute(
            dto.token(),
            mapToUserId(dto.userId()),
            mapToEmail(dto.email()),
            dto.duration(),
            dto.createdAt(),
            dto.expiresAt(),
            dto.confirmedAt()
        );
    }

    public Optional<ConfirmationTokenDto> domainToDto(Optional<ConfirmationToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<ConfirmationToken> dtoToDomain(Optional<ConfirmationTokenDto> dto) {
        return dto.map(this::dtoToDomain);
    }



    private UserId mapToUserId(Long userId) {
        return userId != null ? new UserId(userId) : null;
    }

    private Email mapToEmail(String email) {
        return email != null ? new Email(email) : null;
    }

    private <T, R> R unwrapValue(T valueObject, Function<T, R> extractor) {
        return valueObject != null ? extractor.apply(valueObject) : null;
    }
}