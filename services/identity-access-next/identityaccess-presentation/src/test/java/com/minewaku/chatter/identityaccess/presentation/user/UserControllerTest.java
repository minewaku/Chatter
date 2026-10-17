package com.minewaku.chatter.identityaccess.presentation.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
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

    private static final String BODY = "{\"currentPassword\":\"Old-pass-1!\",\"newPassword\":\"New-pass-2@\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ChangePasswordUseCase changePassword;

    @MockitoBean
    SoftDeleteUserAccountUseCase softDelete;

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
    void closingTheAccountIsNoContentWhetherOrNotItChangedAndUsesOnlyTheCredentialSubject() throws Exception {
        when(softDelete.handle(any()))
                .thenReturn(new SoftDeleteUserAccountUseCase.Result(true))
                .thenReturn(new SoftDeleteUserAccountUseCase.Result(false));

        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/api/v1/users/@me").with(caller()).param("userId", "99"))
                    .andExpect(status().isNoContent());
        }

        ArgumentCaptor<SoftDeleteUserAccountUseCase.Command> captor =
                ArgumentCaptor.forClass(SoftDeleteUserAccountUseCase.Command.class);
        org.mockito.Mockito.verify(softDelete, org.mockito.Mockito.times(2)).handle(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(command -> assertThat(command.userId()).isEqualTo(42L));
    }

    @Test
    void withoutACredentialNothingRuns() throws Exception {
        mvc.perform(delete("/api/v1/users/@me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(put("/api/v1/users/@me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(changePassword, softDelete);
    }
}
