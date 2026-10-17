package com.minewaku.chatter.identityaccess.presentation.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutOtherSessionsUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(SessionController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class SessionControllerTest {

    private static final String PASSWORD_BODY = "{\"currentPassword\":\"S3cure-pass!\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FindSessionsQuery findSessions;

    @MockitoBean
    LogoutSessionUseCase logoutSession;

    @MockitoBean
    LogoutOtherSessionsUseCase logoutOthers;

    private static RequestPostProcessor caller() {
        return jwt().jwt(token -> token.subject("42").claim("sid", "current-session"));
    }

    @Test
    void listReturnsOnlySafeFieldsForTheAuthenticatedAccount() throws Exception {
        Instant issued = Instant.parse("2026-10-01T10:00:00Z");
        when(findSessions.handle(any()))
                .thenReturn(List.of(new FindSessionsQuery.SessionView(
                        "s-1", 42L, "Chrome on Linux (Desktop)", issued, issued.plusSeconds(3600), null, null)));

        mvc.perform(get("/api/v1/sessions").with(caller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("s-1"))
                .andExpect(jsonPath("$[0].deviceInfo").value("Chrome on Linux (Desktop)"))
                .andExpect(jsonPath("$[0].issuedAt").value("2026-10-01T10:00:00Z"))
                .andExpect(jsonPath("$[0].expiresAt").value("2026-10-01T11:00:00Z"))
                .andExpect(jsonPath("$[0].lastUsedAt").doesNotExist())
                .andExpect(jsonPath("$[0].length()").value(6))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("userId"))));

        ArgumentCaptor<FindSessionsQuery.Command> captor = ArgumentCaptor.forClass(FindSessionsQuery.Command.class);
        verify(findSessions).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
    }

    @Test
    void listForAnInaccessibleAccountIsForbiddenNotAServerError() throws Exception {
        when(findSessions.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(get("/api/v1/sessions").with(caller()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inaccessible"));
    }

    @Test
    void revokingOneSessionIsNoContentWhenNewlyRevoked() throws Exception {
        when(logoutSession.handle(any())).thenReturn(new LogoutSessionUseCase.Result(true));

        mvc.perform(post("/api/v1/sessions/target-session/revoke")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_BODY))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<LogoutSessionUseCase.Command> captor =
                ArgumentCaptor.forClass(LogoutSessionUseCase.Command.class);
        verify(logoutSession).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().sessionId()).isEqualTo("target-session");
        assertThat(captor.getValue().currentPassword()).isEqualTo("S3cure-pass!");
    }

    @Test
    void anAbsentOrForeignSessionIsNotFoundWithoutAnyOwnershipHint() throws Exception {
        when(logoutSession.handle(any())).thenReturn(new LogoutSessionUseCase.Result(false));

        mvc.perform(post("/api/v1/sessions/someone-elses/revoke")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("session_not_found"));
    }

    @Test
    void aWrongPasswordIsForbiddenAndNeverUnauthenticated() throws Exception {
        when(logoutSession.handle(any())).thenThrow(new AuthenticationFailedException());
        when(logoutOthers.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/sessions/target-session/revoke")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("reauthentication_failed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("S3cure"))));
        mvc.perform(post("/api/v1/sessions/revoke-others")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("reauthentication_failed"));
    }

    @Test
    void revokingOthersUsesTheSessionFromTheAccessCredentialAndReportsTheCount() throws Exception {
        when(logoutOthers.handle(any())).thenReturn(new LogoutOtherSessionsUseCase.Result(3));

        mvc.perform(post("/api/v1/sessions/revoke-others")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"currentPassword\":\"S3cure-pass!\",\"currentSessionId\":\"forged\",\"userId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedCount").value(3))
                .andExpect(jsonPath("$.length()").value(1));

        ArgumentCaptor<LogoutOtherSessionsUseCase.Command> captor =
                ArgumentCaptor.forClass(LogoutOtherSessionsUseCase.Command.class);
        verify(logoutOthers).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().currentSessionId()).isEqualTo("current-session");
    }

    @Test
    void aMissingPasswordIsAValidationFailure() throws Exception {
        mvc.perform(post("/api/v1/sessions/revoke-others")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        verifyNoInteractions(logoutOthers);
    }

    @Test
    void aCredentialWithoutASessionClaimOrANumericSubjectIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/sessions").with(jwt().jwt(token -> token.subject("42"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(get("/api/v1/sessions")
                        .with(jwt().jwt(token -> token.subject("abc").claim("sid", "s"))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(findSessions);
    }

    @Test
    void withoutACredentialNothingRuns() throws Exception {
        mvc.perform(get("/api/v1/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(post("/api/v1/sessions/revoke-others")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PASSWORD_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(findSessions, logoutSession, logoutOthers);
    }
}
