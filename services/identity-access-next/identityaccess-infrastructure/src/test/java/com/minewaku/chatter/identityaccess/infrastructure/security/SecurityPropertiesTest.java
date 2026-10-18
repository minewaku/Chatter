package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    @Test
    void theEmailChangeConfirmationIsPublicByDefaultBecauseTheTokenIsTheProof() {
        assertThat(new SecurityProperties(null, null).publicEndpoints())
                .contains("/api/v*/users/email-change/confirm", "/api/v*/auth/verification/confirm")
                .doesNotContain("/api/v*/users/@me/email-change", "/api/v*/users/@me/close");
    }
}
