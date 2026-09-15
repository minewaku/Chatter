package com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.mapper;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationTokenId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.model.ConfirmationTokenModel;

@Component
public class RedisConfirmationTokenMapper {

    public ConfirmationTokenModel domainToDto(ConfirmationToken domain) {
        if (domain == null) return null;

        return new ConfirmationTokenModel(
            domain.getId().getValue(),
            unwrapValue(domain.getUserId(), UserId::getValue),
            unwrapValue(domain.getEmail(), Email::getValue),
            domain.getDuration(),
            domain.getCreatedAt(),
            domain.getExpiresAt(),
            domain.getConfirmedAt()
        );
    }

    public ConfirmationToken dtoToDomain(ConfirmationTokenModel dto) {
        if (dto == null) return null;

        return ConfirmationToken.reconstitute(
            new ConfirmationTokenId(dto.token()),
            mapToUserId(dto.userId()),
            mapToEmail(dto.email()),
            dto.duration(),
            dto.createdAt(),
            dto.expiresAt(),
            dto.confirmedAt()
        );
    }

    public Optional<ConfirmationTokenModel> domainToDto(Optional<ConfirmationToken> domain) {
        return domain.map(this::domainToDto);
    }

    public Optional<ConfirmationToken> dtoToDomain(Optional<ConfirmationTokenModel> dto) {
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
