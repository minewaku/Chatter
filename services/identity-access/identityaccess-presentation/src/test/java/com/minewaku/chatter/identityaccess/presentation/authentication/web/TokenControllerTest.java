package com.minewaku.chatter.identityaccess.presentation.authentication.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
    void refreshReadsTheCookieAndReplacesItWithTheNewCredentialKeepingItOutOfTheBody() throws Exception {
        when(refresh.handle(any())).thenReturn(new RefreshAccessTokenUseCase.Refreshed("access-2", "refresh-2"));

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "refresh-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(content().string(not(containsString("refresh-"))))
                .andExpect(header().string("Set-Cookie", startsWith("refresh_token=refresh-2;")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        ArgumentCaptor<RefreshAccessTokenUseCase.Command> captor =
                ArgumentCaptor.forClass(RefreshAccessTokenUseCase.Command.class);
        verify(refresh).handle(captor.capture());
        assertThat(captor.getValue().refreshToken()).isEqualTo("refresh-1");
    }

    @Test
    void aBodyCarriedCredentialIsIgnored() throws Exception {
        when(refresh.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"refresh-1\"}"))
                .andExpect(status().isUnauthorized());

        ArgumentCaptor<RefreshAccessTokenUseCase.Command> captor =
                ArgumentCaptor.forClass(RefreshAccessTokenUseCase.Command.class);
        verify(refresh).handle(captor.capture());
        assertThat(captor.getValue().refreshToken()).isNull();
    }

    @Test
    void aDeniedRenewalIsAGenericUnauthenticatedOutcomeThatDoesNotEchoTheCredentialOrSetACookie() throws Exception {
        when(refresh.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "secret-credential")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void anAbsentOrBlankCookieStillReachesTheUseCaseAndIsDenied() throws Exception {
        when(refresh.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "  ")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"));
    }

    @Test
    void logoutIsAlwaysNoContentAndClearsTheCookieWhetherOrNotASessionWasRevoked() throws Exception {
        when(logout.handle(any()))
                .thenReturn(new LogoutCurrentSessionUseCase.Result(true))
                .thenReturn(new LogoutCurrentSessionUseCase.Result(false));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", "refresh-1")))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""))
                    .andExpect(header().string("Set-Cookie", startsWith("refresh_token=;")))
                    .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                    .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")));
        }
    }

    @Test
    void logoutWithoutACookieIsStillNoContent() throws Exception {
        when(logout.handle(any())).thenReturn(new LogoutCurrentSessionUseCase.Result(false));

        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", "")))
                .andExpect(status().isNoContent());
    }
}
