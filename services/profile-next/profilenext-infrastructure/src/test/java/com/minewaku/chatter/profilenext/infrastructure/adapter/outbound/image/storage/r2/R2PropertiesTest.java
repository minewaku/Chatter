package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.r2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class R2PropertiesTest {

    @Test
    void theEndpointIsDerivedFromTheAccountId() {
        R2Properties properties = new R2Properties("abc123", null, null, "media", "k", "s", null, null);

        assertThat(properties.endpoint()).isEqualTo("https://abc123.r2.cloudflarestorage.com");
        assertThat(properties.region()).isEqualTo("auto");
    }

    @Test
    void anExplicitEndpointWinsAndLosesItsTrailingSlash() {
        R2Properties properties = new R2Properties("abc123", "https://abc123.eu.r2.cloudflarestorage.com/", null, "media", "k", "s", null, null);

        assertThat(properties.endpoint()).isEqualTo("https://abc123.eu.r2.cloudflarestorage.com");
    }

    @Test
    void emptySettingsLeaveTheStorageUnconfigured() {
        assertThat(new R2Properties(null, null, null, null, null, null, null, null).credentialsConfigured()).isFalse();
        assertThat(new R2Properties("abc", null, null, "media", "", "s", null, null).credentialsConfigured()).isFalse();
        assertThat(new R2Properties("abc", null, null, "", "k", "s", null, null).credentialsConfigured()).isFalse();
        assertThat(new R2Properties("", null, null, "media", "k", "s", null, null).credentialsConfigured()).isFalse();
        assertThat(new R2Properties("abc", null, null, "media", "k", "s", null, null).credentialsConfigured()).isTrue();
    }

    @Test
    void thePrintedFormNeverShowsTheKeys() {
        R2Properties properties = new R2Properties("abc", null, null, "media", "access-123", "secret-456", null, null);

        assertThat(properties.toString()).doesNotContain("access-123").doesNotContain("secret-456");
    }
}
