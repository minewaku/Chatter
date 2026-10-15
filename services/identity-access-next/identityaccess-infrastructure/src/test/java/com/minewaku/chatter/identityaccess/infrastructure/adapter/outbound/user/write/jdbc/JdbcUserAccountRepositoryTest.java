package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.support.AccountFixtures;
import com.minewaku.chatter.identityaccess.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.identityaccess.user.internal.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

@SpringBootTest(classes = JdbcTestConfiguration.class)
class JdbcUserAccountRepositoryTest extends PostgreSqlTestSupport {

    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() * 1000);

    @Autowired
    JdbcUserAccountRepository repository;

    private static long nextId() {
        return IDS.incrementAndGet();
    }

    private UserAccount newAccount(String label) {
        long id = nextId();
        return AccountFixtures.pendingAccount(id, label + id + "@example.com", label + id);
    }

    @Test
    void savedAccountIsFoundByIdEmailAndUsername() {
        UserAccount saved = repository.save(newAccount("alice"));

        assertThat(repository.findById(saved.id()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(saved);
        assertThat(repository.findByEmail(saved.email()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(saved);
        assertThat(repository.findByUsername(saved.username()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(saved);
    }

    @Test
    void unknownLookupsAreEmpty() {
        assertThat(repository.findById(new UserId(1L))).isEmpty();
        assertThat(repository.findByEmail(new Email("nobody@example.com"))).isEmpty();
        assertThat(repository.findByUsername(new Username("nobody"))).isEmpty();
    }

    @Test
    void firstInsertHasVersionZeroAndEachUpdateIncrementsIt() {
        UserAccount first = repository.save(newAccount("versioned"));
        assertThat(first.version()).isEqualTo(0L);

        UserAccount second = repository.save(first);
        assertThat(second.version()).isEqualTo(1L);
    }

    @Test
    void duplicateEmailIsReportedAsDuplicateAccount() {
        UserAccount existing = repository.save(newAccount("dupemail"));
        long id = nextId();
        UserAccount clash = AccountFixtures.pendingAccount(id, existing.email().value(), "other" + id);

        assertThatThrownBy(() -> repository.save(clash))
                .isInstanceOf(DuplicateUserAccountException.class)
                .hasCauseInstanceOf(Exception.class)
                .hasMessageNotContaining("insert");
    }

    @Test
    void duplicateUsernameIsReportedAsDuplicateAccount() {
        UserAccount existing = repository.save(newAccount("dupname"));
        long id = nextId();
        UserAccount clash = AccountFixtures.pendingAccount(
                id, "other" + id + "@example.com", existing.username().value());

        assertThatThrownBy(() -> repository.save(clash)).isInstanceOf(DuplicateUserAccountException.class);
    }

    @Test
    void concurrentRegistrationsOfTheSameEmailStoreExactlyOneAccount() throws Exception {
        String email = "race" + nextId() + "@example.com";
        UserAccount a = AccountFixtures.pendingAccount(nextId(), email, "racea" + nextId());
        UserAccount b = AccountFixtures.pendingAccount(nextId(), email, "raceb" + nextId());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> fa = pool.submit(attempt(a, start));
            Future<Boolean> fb = pool.submit(attempt(b, start));
            start.countDown();

            int successes = (fa.get() ? 1 : 0) + (fb.get() ? 1 : 0);

            assertThat(successes).isEqualTo(1);
            assertThat(repository.findByEmail(new Email(email))).isPresent();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void staleWriteIsRejected() {
        UserAccount saved = repository.save(newAccount("stale"));
        UserAccount copyOne = repository.findById(saved.id()).orElseThrow();
        UserAccount copyTwo = repository.findById(saved.id()).orElseThrow();

        repository.save(copyOne);

        assertThatThrownBy(() -> repository.save(copyTwo)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    private Callable<Boolean> attempt(UserAccount account, CountDownLatch start) {
        return () -> {
            start.await();
            try {
                repository.save(account);
                return true;
            } catch (DuplicateUserAccountException e) {
                return false;
            }
        };
    }
}
