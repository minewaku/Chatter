package com.minewaku.chatter.identityaccess.presentation.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ChangeUsernameUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RequestEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(UserController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class UserControllerTest {

    private static final String CLOSE_BODY = "{\"currentPassword\":\"Old-pass-1!\"}";
    private static final String BODY = "{\"currentPassword\":\"Old-pass-1!\",\"newPassword\":\"New-pass-2@\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ChangePasswordUseCase changePassword;

    @MockitoBean
    SoftDeleteUserAccountUseCase softDelete;

    @MockitoBean
    ChangeUsernameUseCase changeUsername;

    @MockitoBean
    RequestEmailChangeUseCase requestEmailChange;

    private static RequestPostProcessor caller() {
        return jwt().jwt(token -> token.subject("42").claim("sid", "current-session"));
    }

    @Test
    void aChangedPasswordIsNoContentWithNeitherPasswordInTheResponse() throws Exception {
        when(changePassword.handle(any())).thenReturn(new ChangePasswordUseCase.Result(true));

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<ChangePasswordUseCase.Command> captor =
                ArgumentCaptor.forClass(ChangePasswordUseCase.Command.class);
        verify(changePassword).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().currentPassword()).isEqualTo("Old-pass-1!");
        assertThat(captor.getValue().newPassword()).isEqualTo("New-pass-2@");
    }

    @Test
    void aNoOpChangeIsAConflict() throws Exception {
        when(changePassword.handle(any())).thenReturn(new ChangePasswordUseCase.Result(false));

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("password_unchanged"));
    }

    @Test
    void aWrongCurrentPasswordIsForbiddenNotUnauthenticated() throws Exception {
        when(changePassword.handle(any())).thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("reauthentication_failed"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old-pass"))));
    }

    @Test
    void anInaccessibleAccountIsForbiddenWithItsOwnCode() throws Exception {
        when(changePassword.handle(any()))
                .thenThrow(new InvalidAccountStateTransitionException(
                        com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus.SUSPENDED,
                        "change password"));

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inaccessible"));
    }

    @Test
    void aWeakNewPasswordNamesTheFieldButNeverEchoesTheValue() throws Exception {
        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Old-pass-1!\",\"newPassword\":\"weakpw1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("weakpw1"))));
        verifyNoInteractions(changePassword);
    }

    @Test
    void aValuePassingTheFormShapeButRejectedByCoreIsAGenericValidationFailure() throws Exception {
        when(changePassword.handle(any())).thenThrow(new IllegalArgumentException("Invalid password format"));

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Invalid password format"))));
    }

    @Test
    void closingTheAccountIsNoContentAndUsesOnlyTheCredentialSubjectAndTheBodyPassword() throws Exception {
        when(softDelete.handle(any())).thenReturn(new SoftDeleteUserAccountUseCase.Result(true));

        mvc.perform(post("/api/v1/users/@me/close")
                        .with(caller())
                        .param("userId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<SoftDeleteUserAccountUseCase.Command> captor =
                ArgumentCaptor.forClass(SoftDeleteUserAccountUseCase.Command.class);
        verify(softDelete).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().currentPassword()).isEqualTo("Old-pass-1!");
    }

    @Test
    void aWrongPasswordWhenClosingIsForbiddenNotUnauthenticatedAndNeverEchoed() throws Exception {
        when(softDelete.handle(any())).thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        mvc.perform(post("/api/v1/users/@me/close")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("reauthentication_failed"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old-pass"))));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"{}", "{\"currentPassword\":\"\"}", "{\"currentPassword\":\"   \"}", "{\"currentPassword\":null}"
            })
    void closingWithoutAUsablePasswordIsAValidationFailureAndNothingRuns(String body) throws Exception {
        mvc.perform(post("/api/v1/users/@me/close")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        verifyNoInteractions(softDelete);
    }

    @Test
    void anOversizePasswordWhenClosingIsAValidationFailure() throws Exception {
        mvc.perform(post("/api/v1/users/@me/close")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + "x".repeat(129) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verifyNoInteractions(softDelete);
    }

    @Test
    void theOldBodylessCloseNoLongerExists() throws Exception {
        mvc.perform(delete("/api/v1/users/@me").with(caller()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        verifyNoInteractions(softDelete);
    }

    @Test
    void aChangedUsernameIsNoContentAndUsesOnlyTheCredentialSubject() throws Exception {
        when(changeUsername.handle(any())).thenReturn(new ChangeUsernameUseCase.Result(true));

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .param("userId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<ChangeUsernameUseCase.Command> captor =
                ArgumentCaptor.forClass(ChangeUsernameUseCase.Command.class);
        verify(changeUsername).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().newUsername()).isEqualTo("new_name");
    }

    @Test
    void anUnchangedUsernameIsAConflictWithItsOwnCode() throws Exception {
        when(changeUsername.handle(any())).thenReturn(new ChangeUsernameUseCase.Result(false));

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("username_unchanged"));
    }

    @Test
    void aUsernameHeldByAnotherAccountIsAConflictThatNamesNobody() throws Exception {
        when(changeUsername.handle(any())).thenThrow(new DuplicateUserAccountException("duplicate", null));

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"taken_name\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_already_exists"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("taken_name"))));
    }

    @Test
    void aUsernameChangeOnAnInaccessibleAccountIsForbiddenWithItsOwnCode() throws Exception {
        when(changeUsername.handle(any()))
                .thenThrow(new InvalidAccountStateTransitionException(
                        com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus.SUSPENDED,
                        "change username"));

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inaccessible"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"username\":\"\"}", "{\"username\":\"   \"}", "{\"username\":null}"})
    void aMissingOrBlankUsernameIsAValidationFailureNamingTheField(String body) throws Exception {
        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("username"));
        verifyNoInteractions(changeUsername);
    }

    @Test
    void anOversizeUsernameIsRejectedWithoutEchoingIt() throws Exception {
        String tooLong = "n".repeat(65);

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(tooLong))));
        verifyNoInteractions(changeUsername);
    }

    @Test
    void aValueRejectedByCoreRulesIsAGenericValidationFailure() throws Exception {
        when(changeUsername.handle(any())).thenThrow(new IllegalArgumentException("Invalid username format"));

        mvc.perform(put("/api/v1/users/@me/username")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\".bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Invalid username format"))));
    }

    private static final String EMAIL_BODY = "{\"newEmail\":\"new@example.com\",\"currentPassword\":\"Old-pass-1!\"}";

    @Test
    void anAcceptedEmailChangeIsAcceptedWithNoBodyAndUsesOnlyTheCredentialSubject() throws Exception {
        when(requestEmailChange.handle(any())).thenReturn(new RequestEmailChangeUseCase.Result(true));

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .param("userId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        ArgumentCaptor<RequestEmailChangeUseCase.Command> captor =
                ArgumentCaptor.forClass(RequestEmailChangeUseCase.Command.class);
        verify(requestEmailChange).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().currentPassword()).isEqualTo("Old-pass-1!");
        assertThat(captor.getValue().newEmail()).isEqualTo("new@example.com");
    }

    @Test
    void anUnchangedEmailIsAConflictWithItsOwnCode() throws Exception {
        when(requestEmailChange.handle(any())).thenReturn(new RequestEmailChangeUseCase.Result(false));

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_unchanged"));
    }

    @Test
    void anEmailHeldByAnotherAccountIsAConflictThatNamesNobody() throws Exception {
        when(requestEmailChange.handle(any())).thenThrow(new DuplicateUserAccountException("duplicate", null));

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_already_exists"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("new@example.com"))));
    }

    @Test
    void aWrongPasswordWhenChangingTheEmailIsForbiddenNotUnauthenticatedAndNeverEchoed() throws Exception {
        when(requestEmailChange.handle(any()))
                .thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("reauthentication_failed"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old-pass"))));
    }

    @Test
    void anEmailChangeOnAnInaccessibleAccountIsForbiddenWithItsOwnCode() throws Exception {
        when(requestEmailChange.handle(any()))
                .thenThrow(new InvalidAccountStateTransitionException(
                        com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus.SUSPENDED,
                        "change email"));

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inaccessible"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"newEmail\":\"nope\",\"currentPassword\":\"Old-pass-1!\"}",
                "{\"newEmail\":\"\",\"currentPassword\":\"Old-pass-1!\"}",
                "{\"currentPassword\":\"Old-pass-1!\"}",
                "{\"newEmail\":\"new@example.com\",\"currentPassword\":\"\"}",
                "{\"newEmail\":\"new@example.com\"}"
            })
    void aMalformedEmailOrABlankPasswordIsAValidationFailureAndNothingRuns(String body) throws Exception {
        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").exists());
        verifyNoInteractions(requestEmailChange);
    }

    @Test
    void anOversizeEmailIsRejectedWithoutEchoingIt() throws Exception {
        String tooLong = "a".repeat(250) + "@example.com";

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"" + tooLong + "\",\"currentPassword\":\"Old-pass-1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newEmail"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(tooLong))));
        verifyNoInteractions(requestEmailChange);
    }

    @Test
    void withoutACredentialNothingRuns() throws Exception {
        mvc.perform(post("/api/v1/users/@me/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(put("/api/v1/users/@me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/users/@me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/@me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(changePassword, softDelete, changeUsername, requestEmailChange);
    }

    @Test
    void aThrottledPasswordConfirmationIsTooManyAttemptsAndNeverUnauthenticated() throws Exception {
        TooManyAttemptsException throttled = new TooManyAttemptsException(java.time.Duration.ofSeconds(300));
        when(changePassword.handle(any())).thenThrow(throttled);
        when(softDelete.handle(any())).thenThrow(throttled);
        when(requestEmailChange.handle(any())).thenThrow(throttled);

        mvc.perform(put("/api/v1/users/@me/password")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_attempts"))
                .andExpect(header().string("Retry-After", "300"));
        mvc.perform(post("/api/v1/users/@me/close")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLOSE_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_attempts"))
                .andExpect(header().string("Retry-After", "300"));
        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"new@example.com\",\"currentPassword\":\"Old-pass-1!\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_attempts"))
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old-pass"))));
    }

    @Test
    void anEmailChangeThatCouldNotBeMailedIsADistinctServiceUnavailable() throws Exception {
        when(requestEmailChange.handle(any())).thenThrow(new MailDeliveryException());

        mvc.perform(post("/api/v1/users/@me/email-change")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"new@example.com\",\"currentPassword\":\"Old-pass-1!\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("mail_unavailable"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("new@"))));
    }
}
