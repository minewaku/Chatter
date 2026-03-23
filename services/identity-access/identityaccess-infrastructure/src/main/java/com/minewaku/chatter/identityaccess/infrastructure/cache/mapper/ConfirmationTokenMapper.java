package com.minewaku.chatter.identityaccess.infrastructure.cache.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.cache.dto.ConfirmationTokenCacheDto;

@Component
public class ConfirmationTokenMapper {

    public ConfirmationTokenCacheDto domainToDto(ConfirmationToken domain) {
        if (domain == null) return null;

        return new ConfirmationTokenCacheDto(
            domain.getToken(),
            unwrapValue(domain.getUserId(), UserId::getValue),
            unwrapValue(domain.getEmail(), Email::getValue),
            domain.getDuration(),
            domain.getCreatedAt(),
            domain.getExpiresAt(),
            domain.getConfirmedAt()
        );
    }

    public ConfirmationToken dtoToDomain(ConfirmationTokenCacheDto dto) {
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

    public Optional<ConfirmationTokenCacheDto> domainToDto(Optional<ConfirmationToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<ConfirmationToken> dtoToDomain(Optional<ConfirmationTokenCacheDto> dto) {
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
