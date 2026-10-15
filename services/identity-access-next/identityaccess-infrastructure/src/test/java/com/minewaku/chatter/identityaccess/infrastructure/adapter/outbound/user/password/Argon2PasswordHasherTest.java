package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.password;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class Argon2PasswordHasherTest {

    private static final String RAW = "S3cure-pass!";

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void hashCarriesAlgorithmLabelHashAndSixteenByteSalt() {
        PasswordHash hash = hasher.hash(new PlainPassword(RAW));

        assertThat(hash.algorithm()).isEqualTo("argon2id");
        assertThat(hash.hash()).startsWith("$argon2id$").doesNotContain(RAW);
        assertThat(hash.salt()).hasSize(16);
    }

    @Test
    void hashingTwiceGivesDifferentSaltAndHash() {
        PasswordHash first = hasher.hash(new PlainPassword(RAW));
        PasswordHash second = hasher.hash(new PlainPassword(RAW));

        assertThat(first.salt()).isNotEqualTo(second.salt());
        assertThat(first.hash()).isNotEqualTo(second.hash());
    }

    @Test
    void correctPasswordMatchesAndWrongPasswordDoesNot() {
        PasswordHash hash = hasher.hash(new PlainPassword(RAW));

        assertThat(hasher.matches(new PlainPassword(RAW), hash)).isTrue();
        assertThat(hasher.matches(new PlainPassword("Wr0ng-password!"), hash)).isFalse();
    }

    @Test
    void otherAlgorithmLabelNeverMatchesAndNeverThrows() {
        PasswordHash foreign = new PasswordHash("bcrypt", "$2a$10$abcdefghijklmnopqrstuv", new byte[] {1, 2, 3});

        assertThat(hasher.matches(new PlainPassword(RAW), foreign)).isFalse();
    }

    @Test
    void neverLogsThePasswordOrTheHash(CapturedOutput output) {
        PasswordHash hash = hasher.hash(new PlainPassword(RAW));
        hasher.matches(new PlainPassword(RAW), hash);

        assertThat(output.getAll()).doesNotContain(RAW).doesNotContain(hash.hash());
    }
}
