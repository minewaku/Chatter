package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.emailchange.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PendingEmailChange;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PendingEmailChangeRecordMapperTest {

    private final PendingEmailChangeRecordMapper mapper = new PendingEmailChangeRecordMapper();

    private PendingEmailChange change(long userId, String rawToken) {
        return PendingEmailChange.issue(
                rawToken,
                new UserId(userId),
                new Email("new@example.com"),
                Clock.fixed(AccountFixtures.NOW, ZoneOffset.UTC),
                Duration.ofHours(24));
    }

    @Test
    void anOutstandingChangeRoundTripsWithoutAConfirmedTime() {
        PendingEmailChange original = change(5L, "raw-token-1");

        PendingEmailChangeRecord record = mapper.toRecord(original);
        PendingEmailChange restored = mapper.toModel(PendingEmailChangeRecord.fromHash(record.toHash()), "raw-token-1");

        assertThat(record.confirmedAt()).isNull();
        assertThat(record.toHash()).doesNotContainKey("verifiedAt");
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void aConfirmedChangeKeepsItsConfirmedTimeUnderTheSharedScriptField() {
        PendingEmailChange original = change(6L, "raw-token-2");
        Instant confirmedAt = AccountFixtures.NOW.plusSeconds(60);
        original.confirm(confirmedAt);

        PendingEmailChangeRecord record = mapper.toRecord(original);
        PendingEmailChange restored = mapper.toModel(PendingEmailChangeRecord.fromHash(record.toHash()), "raw-token-2");

        assertThat(record.toHash()).containsKey("verifiedAt");
        assertThat(restored.confirmedAt()).isEqualTo(confirmedAt);
        assertThat(restored.newEmail()).isEqualTo(new Email("new@example.com"));
    }

    @Test
    void theRecordNeverHoldsTheRawToken() {
        PendingEmailChangeRecord record = mapper.toRecord(change(7L, "very-secret-raw-token"));

        assertThat(Arrays.stream(PendingEmailChangeRecord.class.getRecordComponents())
                        .map(RecordComponent::getName))
                .doesNotContain("rawToken", "token");
        assertThat(record.toHash().values()).noneMatch(value -> value.contains("very-secret-raw-token"));
    }
}
