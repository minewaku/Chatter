package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StartSmokeE2eTest extends E2eSupport {

    @Test
    void theWholeServiceStartsAndReportsItselfHealthy() {
        ApiResponse health = api().getRoot("/actuator/health");

        assertThat(health.status()).isEqualTo(200);
        assertThat(health.text("status")).isEqualTo("UP");
    }
}
