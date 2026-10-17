package com.minewaku.chatter.identityaccess.presentation.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeviceLabelResolverTest {

    private static final DeviceLabelResolver RESOLVER = new DeviceLabelResolver(UserAgentConfiguration.newAnalyzer());

    @Test
    void describesBrowserSystemAndDeviceClass() {
        String label = RESOLVER.resolve("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/130.0.0.0 Safari/537.36");

        assertThat(label).isEqualTo("Chrome on Linux (Desktop)");
    }

    @Test
    void missingOrBlankHeaderFallsBackToUnknownDevice() {
        assertThat(RESOLVER.resolve(null)).isEqualTo("Unknown device");
        assertThat(RESOLVER.resolve("   ")).isEqualTo("Unknown device");
    }

    @Test
    void unparseableAgentStillGivesANonBlankLabel() {
        String label = RESOLVER.resolve("xyz");

        assertThat(label).isNotBlank().hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void veryLongAgentStringIsTruncatedToTheDeviceLabelLimit() {
        String label = RESOLVER.resolve("A".repeat(5_000));

        assertThat(label).isNotBlank().hasSizeLessThanOrEqualTo(200);
    }
}
