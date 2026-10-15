package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.authentication.internal.model.AuthenticationSession;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SessionRecordMapperTest {

    private final SessionRecordMapper mapper = new SessionRecordMapper();
    private final Instant issuedAt = Instant.parse("2026-10-15T10:15:30.123456Z");

    @Test
    void freshSessionRoundTripsWithAbsentOptionalTimes() {
        AuthenticationSession original =
                AuthenticationSession.issue("s-1", new UserId(10L), "Firefox", issuedAt, Duration.ofDays(30));

        SessionRecord record = mapper.toRecord(original, "digest");
        AuthenticationSession restored = mapper.toModel(SessionRecord.fromHash("s-1", record.toHash()));

        assertThat(record.lastUsedAt()).isNull();
        assertThat(record.revokedAt()).isNull();
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void usedAndRevokedTimesRoundTrip() {
        AuthenticationSession original = AuthenticationSession.reconstitute(
                "s-2",
                new UserId(11L),
                "Chrome",
                issuedAt,
                issuedAt.plus(30, ChronoUnit.DAYS),
                issuedAt.plusSeconds(5),
                issuedAt.plusSeconds(10));

        AuthenticationSession restored = mapper.toModel(SessionRecord.fromHash(
                "s-2", mapper.toRecord(original, "digest").toHash()));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void theRecordHoldsOnlyTheDigestNeverTheRawToken() {
        assertThat(Arrays.stream(SessionRecord.class.getRecordComponents()).map(RecordComponent::getName))
                .contains("tokenDigest")
                .doesNotContain("rawToken", "refreshToken", "token");
    }
}
