package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
class UserAccountRowMapper {

    UserAccountRow toRow(UserAccount account) {
        PasswordHash hash = account.passwordHash();
        return new UserAccountRow(
                account.id().value(),
                account.email().value(),
                account.username().value(),
                account.birthday().value(),
                account.status().name(),
                toOffset(account.deletedAt()),
                hash.algorithm(),
                hash.hash(),
                hash.salt(),
                toOffset(account.createdAt()),
                toOffset(account.updatedAt()),
                account.version());
    }

    UserAccount toModel(UserAccountRow row) {
        return UserAccount.reconstitute(
                new UserId(row.id()),
                new Email(row.normalizedEmail()),
                new Username(row.username()),
                new Birthday(row.birthday()),
                AccountStatus.valueOf(row.status()),
                toInstant(row.deletedAt()),
                new PasswordHash(row.passwordAlgorithm(), row.passwordHash(), row.passwordSalt()),
                toInstant(row.createdAt()),
                toInstant(row.updatedAt()),
                row.version());
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
