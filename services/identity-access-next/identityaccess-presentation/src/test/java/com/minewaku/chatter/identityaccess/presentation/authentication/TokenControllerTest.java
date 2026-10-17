package com.minewaku.chatter.identityaccess.presentation.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TokenController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class TokenControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RefreshAccessTokenUseCase refresh;

    @MockitoBean
    LogoutCurrentSessionUseCase logout;

    @Test
    void refreshReturnsExactlyTheNewCredentialPair() throws Exception {
        when(refresh.handle(any())).thenReturn(new RefreshAccessTokenUseCase.Refreshed("access-2", "refresh-2"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-2"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        ArgumentCaptor<RefreshAccessTokenUseCase.Command> captor =
                ArgumentCaptor.forClass(RefreshAccessTokenUseCase.Command.class);
        verify(refresh).handle(captor.capture());
        assertThat(captor.getValue().refreshToken()).isEqualTo("refresh-1");
    }

    @Test
    void aDeniedRenewalIsAGenericUnauthenticatedOutcomeThatDoesNotEchoTheCredential() throws Exception {
        when(refresh.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"secret-credential\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void anAbsentOrBlankCredentialStillReachesTheUseCaseAndIsDenied() throws Exception {
        when(refresh.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"  \"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"));
    }

    @Test
    void logoutIsAlwaysNoContentWhetherOrNotASessionWasRevoked() throws Exception {
        when(logout.handle(any()))
                .thenReturn(new LogoutCurrentSessionUseCase.Result(true))
                .thenReturn(new LogoutCurrentSessionUseCase.Result(false));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh-1\"}"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
    }

    @Test
    void logoutWithAMissingOrBlankCredentialIsStillNoContent() throws Exception {
        when(logout.handle(any())).thenReturn(new LogoutCurrentSessionUseCase.Result(false));

        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void anOverlongCredentialIsAValidationFailure() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + "a".repeat(513) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
    }
}
