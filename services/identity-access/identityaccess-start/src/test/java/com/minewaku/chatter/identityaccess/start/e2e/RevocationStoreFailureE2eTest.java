package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Runs in its own class because it pauses the Redis container shared by the whole end-to-end suite. The session-store
 * breaker is set to open after three recorded calls and to stay open for three seconds, so the whole cycle fits in a
 * test.
 */
@TestPropertySource(
        properties = {
            "identity-access.resilience.session-store.window-size=3",
            "identity-access.resilience.session-store.minimum-calls=3",
            "identity-access.resilience.session-store.open-duration=PT3S"
        })
class RevocationStoreFailureE2eTest extends E2eSupport {

    @AfterEach
    void restore() {
        try {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        } catch (RuntimeException alreadyRunning) {
            // the test unpaused it
        }
        resetBreakers();
    }

    @Test
    void protectedRequestsFailClosedWhileTheRevocationStoreIsUnreachableAndRecoverAfterwards() {
        String[] tokens = signIn(registerVerifiedAccount());
        assertThat(api().get("/sessions", tokens[0]).status()).isNotEqualTo(401);

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        ApiResponse unavailable;
        long elapsedMillis;
        try {
            long started = System.nanoTime();
            unavailable = api().get("/sessions", tokens[0]);
            elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }

        assertThat(unavailable.status()).isEqualTo(503);
        assertThat(unavailable.text("code")).isEqualTo("service_unavailable");
        assertThat(unavailable.header("Retry-After")).isEqualTo("5");
        assertThat(elapsedMillis).isLessThan(2000);
        assertThat(api().get("/sessions", tokens[0]).status()).isNotEqualTo(401).isNotEqualTo(503);
    }

    @Test
    void afterRepeatedFailuresTheStoreIsHeldOffThenQuestionedAgainOnItsOwn() throws InterruptedException {
        String[] tokens = signIn(registerVerifiedAccount());
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        ApiResponse heldOff;
        long elapsedMillis;
        try {
            for (int i = 0; i < 3; i++) {
                assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(503);
            }
            long started = System.nanoTime();
            heldOff = api().get("/sessions", tokens[0]);
            elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }

        // once held off the store is not waited on at all, and nothing is let through
        assertThat(heldOff.status()).isEqualTo(503);
        assertThat(elapsedMillis).isLessThan(500);
        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(503);

        Thread.sleep(3500);

        assertThat(api().get("/sessions", tokens[0]).status()).isEqualTo(200);
    }
}
