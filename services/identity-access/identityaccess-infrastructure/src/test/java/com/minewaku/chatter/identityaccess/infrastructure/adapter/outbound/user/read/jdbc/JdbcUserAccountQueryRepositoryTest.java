package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.read.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc.JdbcUserAccountRepository;
import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import com.minewaku.chatter.identityaccess.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = JdbcTestConfiguration.class)
class JdbcUserAccountQueryRepositoryTest extends PostgreSqlTestSupport {

    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() * 1000 + 500_000);

    @Autowired
    JdbcUserAccountRepository writer;

    @Autowired
    JdbcUserAccountQueryRepository reader;

    @Autowired
    JdbcClient jdbc;

    @Test
    void resultsCarryTheStoredFields() {
        long id = IDS.incrementAndGet();
        UserAccount saved =
                writer.save(AccountFixtures.pendingAccount(id, "reader" + id + "@example.com", "reader" + id));

        FindUserByIdUseCase.Result byId = reader.findById(new UserId(id)).orElseThrow();
        FindUserByEmailUseCase.Result byEmail =
                reader.findByEmail(new Email("reader" + id + "@example.com")).orElseThrow();

        assertThat(byId.userId()).isEqualTo(id);
        assertThat(byId.email()).isEqualTo(saved.email().value());
        assertThat(byId.username()).isEqualTo(saved.username().value());
        assertThat(byId.birthday()).isEqualTo(saved.birthday().value());
        assertThat(byId.status()).isEqualTo("PENDING_VERIFICATION");
        assertThat(byId.accessible()).isFalse();
        assertThat(byId.deletedAt()).isNull();
        assertThat(byId.createdAt()).isEqualTo(saved.createdAt());
        assertThat(byId.updatedAt()).isEqualTo(saved.updatedAt());
        assertThat(byEmail.userId()).isEqualTo(id);
        assertThat(byEmail.createdAt()).isEqualTo(saved.createdAt());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void accessibleFollowsTheStatus(AccountStatus status) {
        long id = IDS.incrementAndGet();
        Instant deletedAt = status == AccountStatus.DELETED ? AccountFixtures.NOW : null;
        jdbc.sql("insert into user_account (id, normalized_email, username, birthday, status, deleted_at,"
                        + " password_algorithm, password_hash, password_salt, created_at, updated_at)"
                        + " values (:id, :email, :username, date '1990-01-01', :status, :deletedAt,"
                        + " 'argon2id', 'hash', :salt, now(), now())")
                .param("id", id)
                .param("email", "status" + id + "@example.com")
                .param("username", "status" + id)
                .param("status", status.name())
                .param("deletedAt", deletedAt == null ? null : Timestamp.from(deletedAt))
                .param("salt", new byte[] {1, 2})
                .update();

        FindUserByIdUseCase.Result result = reader.findById(new UserId(id)).orElseThrow();

        assertThat(result.status()).isEqualTo(status.name());
        assertThat(result.accessible()).isEqualTo(status.isAccessible());
        assertThat(result.deletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void unknownIdAndEmailAreEmpty() {
        assertThat(reader.findById(new UserId(1L))).isEmpty();
        assertThat(reader.findByEmail(new Email("nobody@example.com"))).isEmpty();
    }
}
