package com.minewaku.chatter.profile.presentation.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ImageUrlResolverTest {

    private static final String PATH = "profiles/42/avatar/0197aaab-0000-7000-8000-000000000001";

    @Test
    void joinsTheBaseAndThePath() {
        assertThat(new ImageUrlResolver("https://media.test").resolve(PATH)).isEqualTo("https://media.test/" + PATH);
    }

    @Test
    void aTrailingSlashOnTheBaseIsNotDoubled() {
        assertThat(new ImageUrlResolver("https://media.test/").resolve(PATH)).isEqualTo("https://media.test/" + PATH);
        assertThat(new ImageUrlResolver("https://media.test///").resolve(PATH)).isEqualTo("https://media.test/" + PATH);
    }

    @Test
    void aBasePathIsKept() {
        assertThat(new ImageUrlResolver("https://cdn.test/img/").resolve(PATH)).isEqualTo("https://cdn.test/img/" + PATH);
    }

    @Test
    void noPathMeansNoImageAndGivesNull() {
        assertThat(new ImageUrlResolver("https://media.test").resolve(null)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/", " /// "})
    void aMissingBaseAddressStopsStartUp(String publicBaseUrl) {
        assertThatThrownBy(() -> new ImageUrlResolver(publicBaseUrl))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROFILE_PUBLIC_BASE_URL");
    }
}
