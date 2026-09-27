package com.minewaku.chatter.identityaccess.user.api.query;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface FindUserByEmailUseCase
        extends UseCaseHandler<FindUserByEmailUseCase.Query, Optional<FindUserByEmailUseCase.Result>> {

    record Query(String email) {}

    record Result(
            long userId,
            String email,
            String username,
            LocalDate birthday,
            String status,
            boolean accessible,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt) {}
}
