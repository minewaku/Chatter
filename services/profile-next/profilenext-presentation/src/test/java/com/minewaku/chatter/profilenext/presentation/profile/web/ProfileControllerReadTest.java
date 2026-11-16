package com.minewaku.chatter.profilenext.presentation.profile.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.profilenext.presentation.image.ImageUrlResolver;
import com.minewaku.chatter.profilenext.presentation.support.TestSecurityConfiguration;
import com.minewaku.chatter.profilenext.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@Import({TestSecurityConfiguration.class, ImageUrlResolver.class})
@TestPropertySource(properties = "profile-next.images.public-base-url=https://img.test")
class ProfileControllerReadTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetProfileUseCase getProfile;

    @MockitoBean
    private SaveProfileUseCase saveProfile;

    private static GetProfileUseCase.Result result(long userId) {
        return new GetProfileUseCase.Result(
                userId,
                "Mine",
                "Hello",
                new GetProfileUseCase.ImageResult("abc", "profiles/" + userId + "/avatar/abc"),
                null,
                Instant.parse("2026-11-01T10:00:00Z"),
                null);
    }

    @Test
    void noTokenIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/profiles/@me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        verifyNoInteractions(getProfile);
    }

    @Test
    void atMeReadsTheProfileOfTheTokenSubject() throws Exception {
        when(getProfile.handle(any())).thenReturn(result(123));

        mvc.perform(get("/api/v1/profiles/@me").with(jwt().jwt(token -> token.subject("123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(123))
                .andExpect(jsonPath("$.displayName").value("Mine"))
                .andExpect(jsonPath("$.bio").value("Hello"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img.test/profiles/123/avatar/abc"))
                .andExpect(jsonPath("$.bannerUrl").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        ArgumentCaptor<GetProfileUseCase.Query> query = ArgumentCaptor.forClass(GetProfileUseCase.Query.class);
        Mockito.verify(getProfile).handle(query.capture());
        org.assertj.core.api.Assertions.assertThat(query.getValue().userId()).isEqualTo(123);
    }

    @Test
    void nullsAreWrittenNotOmitted() throws Exception {
        when(getProfile.handle(any())).thenReturn(result(5));

        String body = mvc.perform(get("/api/v1/profiles/5").with(jwt().jwt(token -> token.subject("1"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("\"bannerUrl\":null")
                .contains("\"updatedAt\":null");
    }

    @Test
    void anIdReadsThatUsersProfile() throws Exception {
        when(getProfile.handle(any())).thenReturn(result(456));

        mvc.perform(get("/api/v1/profiles/456").with(jwt().jwt(token -> token.subject("123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(456));

        ArgumentCaptor<GetProfileUseCase.Query> query = ArgumentCaptor.forClass(GetProfileUseCase.Query.class);
        Mockito.verify(getProfile).handle(query.capture());
        org.assertj.core.api.Assertions.assertThat(query.getValue().userId()).isEqualTo(456);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-5", "99999999999999999999"})
    void aBadIdIsAValidationFailureNeverANotFound(String id) throws Exception {
        mvc.perform(get("/api/v1/profiles/" + id).with(jwt().jwt(token -> token.subject("123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("userId"));
        verifyNoInteractions(getProfile);
    }
}
