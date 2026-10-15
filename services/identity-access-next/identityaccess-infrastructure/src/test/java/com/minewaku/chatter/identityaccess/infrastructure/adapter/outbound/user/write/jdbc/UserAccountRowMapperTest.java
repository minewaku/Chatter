package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserAccountRowMapperTest {

    private final UserAccountRowMapper mapper = new UserAccountRowMapper();

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void roundTripReturnsAnEqualModelForEveryStatus(AccountStatus status) {
        Instant deletedAt = status == AccountStatus.DELETED ? AccountFixtures.NOW : null;
        UserAccount original = AccountFixtures.reconstitutedAccount(42L, status, deletedAt);

        UserAccount restored = mapper.toModel(mapper.toRow(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void newAccountKeepsNullVersion() {
        UserAccount account = AccountFixtures.pendingAccount(7L, "new@example.com", "newuser");

        assertThat(mapper.toRow(account).version()).isNull();
    }

    @Test
    void passwordMaterialIsPreservedByteForByte() {
        UserAccount original = AccountFixtures.reconstitutedAccount(9L, AccountStatus.ACTIVE, null);

        UserAccountRow row = mapper.toRow(original);

        assertThat(row.passwordAlgorithm()).isEqualTo(original.passwordHash().algorithm());
        assertThat(row.passwordHash()).isEqualTo(original.passwordHash().hash());
        assertThat(row.passwordSalt()).containsExactly(original.passwordHash().salt());
    }

    @Test
    void nanosecondsAreTruncatedToMicrosecondsOnWrite() {
        UserAccount account = AccountFixtures.pendingAccount(11L, "nanos@example.com", "nanouser");

        UserAccountRow row = mapper.toRow(account);

        assertThat(row.createdAt().getNano() % 1_000).isZero();
        assertThat(row.updatedAt().getNano() % 1_000).isZero();
    }
}
