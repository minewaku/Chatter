package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Runs in its own class because it pauses the Redis container shared by the whole end-to-end suite. */
class RevocationStoreFailureE2eTest extends E2eSupport {

    @Test
    void protectedRequestsFailClosedWhileTheRevocationStoreIsUnreachableAndRecoverAfterwards() {
        String[] tokens = signIn(registerVerifiedAccount());
        assertThat(api().get("/sessions", tokens[0]).status()).isNotEqualTo(401);

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        ApiResponse unavailable;
        try {
            unavailable = api().get("/sessions", tokens[0]);
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }

        assertThat(unavailable.status()).isEqualTo(503);
        assertThat(unavailable.text("code")).isEqualTo("service_unavailable");
        assertThat(unavailable.header("Retry-After")).isEqualTo("5");
        assertThat(api().get("/sessions", tokens[0]).status()).isNotEqualTo(401).isNotEqualTo(503);
    }
}
