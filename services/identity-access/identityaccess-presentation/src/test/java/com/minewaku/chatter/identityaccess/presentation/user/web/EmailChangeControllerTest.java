package com.minewaku.chatter.identityaccess.presentation.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import com.minewaku.chatter.identityaccess.user.api.command.ConfirmEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmailChangeController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class EmailChangeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ConfirmEmailChangeUseCase confirmEmailChange;

    @Test
    void confirmationReportsConfirmedAndPassesTheTokenOn() throws Exception {
        when(confirmEmailChange.handle(any())).thenReturn(new ConfirmEmailChangeUseCase.Confirmed(7L));

        mvc.perform(get("/api/v1/users/email-change/confirm").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        ArgumentCaptor<ConfirmEmailChangeUseCase.Command> captor =
                ArgumentCaptor.forClass(ConfirmEmailChangeUseCase.Command.class);
        verify(confirmEmailChange).handle(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().token()).isEqualTo("abc");
    }

    @Test
    void repeatedConfirmationReportsAlreadyConfirmed() throws Exception {
        when(confirmEmailChange.handle(any())).thenReturn(new ConfirmEmailChangeUseCase.AlreadyConfirmed());

        mvc.perform(get("/api/v1/users/email-change/confirm").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("already_confirmed"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void anInvalidOrExpiredTokenIsAClientError() throws Exception {
        when(confirmEmailChange.handle(any())).thenReturn(new ConfirmEmailChangeUseCase.InvalidOrExpired());

        mvc.perform(get("/api/v1/users/email-change/confirm").param("token", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
    }

    @Test
    void aMissingOrBlankTokenIsInvalidWithoutCallingTheUseCase() throws Exception {
        mvc.perform(get("/api/v1/users/email-change/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
        mvc.perform(get("/api/v1/users/email-change/confirm").param("token", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
        verifyNoInteractions(confirmEmailChange);
    }

    @Test
    void anAddressTakenInTheMeantimeIsAConflict() throws Exception {
        when(confirmEmailChange.handle(any())).thenThrow(new DuplicateUserAccountException("duplicate", null));

        mvc.perform(get("/api/v1/users/email-change/confirm").param("token", "abc"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_already_exists"));
    }
}
