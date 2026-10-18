package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VerificationRecordMapperTest {

    private final VerificationRecordMapper mapper = new VerificationRecordMapper();

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void outstandingTokenRoundTripsWithoutAVerifiedTime(VerificationPurpose purpose) {
        EmailVerification original = AccountFixtures.verification(purpose, 5L, "raw-token-1");

        VerificationRecord record = mapper.toRecord(original);
        EmailVerification restored =
                mapper.toModel(VerificationRecord.fromHash(record.toHash()), original.purpose(), "raw-token-1");

        assertThat(record.verifiedAt()).isNull();
        assertThat(restored.purpose()).isEqualTo(original.purpose());
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void consumedTokenKeepsItsVerifiedTime(VerificationPurpose purpose) {
        EmailVerification original = AccountFixtures.verification(purpose, 6L, "raw-token-2");
        Instant verifiedAt = AccountFixtures.NOW.plusSeconds(60);
        original.verify(verifiedAt);

        VerificationRecord record = mapper.toRecord(original);
        EmailVerification restored =
                mapper.toModel(VerificationRecord.fromHash(record.toHash()), original.purpose(), "raw-token-2");

        assertThat(record.verifiedAt()).isNotNull();
        assertThat(restored.verifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void theRecordNeverHoldsTheRawToken() {
        EmailVerification original = AccountFixtures.verification(7L, "very-secret-raw-token");

        VerificationRecord record = mapper.toRecord(original);

        assertThat(Arrays.stream(VerificationRecord.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("rawToken", "token");
        assertThat(record.toHash().values()).noneMatch(value -> value.contains("very-secret-raw-token"));
    }
}
