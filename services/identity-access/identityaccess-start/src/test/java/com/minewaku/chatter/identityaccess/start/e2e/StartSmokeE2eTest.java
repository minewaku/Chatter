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

    @Test
    void theHealthEndpointShowsMailAndTheSessionStoreWithNoDetails() {
        ApiResponse health = api().getRoot("/actuator/health");

        assertThat(health.json()
                        .get("components")
                        .get("mailDelivery")
                        .get("status")
                        .asString())
                .isEqualTo("UP");
        assertThat(health.json()
                        .get("components")
                        .get("sessionStore")
                        .get("status")
                        .asString())
                .isEqualTo("UP");
        assertThat(health.json().get("components").get("mailDelivery").has("details"))
                .isFalse();
        assertThat(health.json().get("components").get("sessionStore").has("details"))
                .isFalse();
    }

    @Test
    void anOpenBreakerShowsAsDegradedWithoutTakingTheServiceDown() {
        breakers.circuitBreaker("mailBreaker").transitionToOpenState();
        try {
            ApiResponse health = api().getRoot("/actuator/health");

            assertThat(health.status()).isEqualTo(200);
            assertThat(health.json()
                            .get("components")
                            .get("mailDelivery")
                            .get("status")
                            .asString())
                    .isEqualTo("DEGRADED");
            assertThat(health.text("status")).isEqualTo("DEGRADED");
        } finally {
            resetBreakers();
        }
    }
}
