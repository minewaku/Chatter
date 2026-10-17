package com.minewaku.chatter.identityaccess.presentation.authentication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificationController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class VerificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RequestEmailVerificationUseCase requestVerification;

    @MockitoBean
    VerifyEmailUseCase verifyEmail;

    @Test
    void resendIsAcceptedWithNoBodyForAWellFormedEmail() throws Exception {
        when(requestVerification.handle(any())).thenReturn(new RequestEmailVerificationUseCase.Accepted());

        mvc.perform(post("/api/v1/auth/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void resendRejectsAMalformedEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        verifyNoInteractions(requestVerification);
    }

    @Test
    void confirmationReportsVerified() throws Exception {
        when(verifyEmail.handle(any())).thenReturn(new VerifyEmailUseCase.Verified(7L));

        mvc.perform(get("/api/v1/auth/verification/confirm").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("verified"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void repeatedConfirmationReportsAlreadyVerified() throws Exception {
        when(verifyEmail.handle(any())).thenReturn(new VerifyEmailUseCase.AlreadyVerified());

        mvc.perform(get("/api/v1/auth/verification/confirm").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("already_verified"));
    }

    @Test
    void invalidOrExpiredTokenIsAClientError() throws Exception {
        when(verifyEmail.handle(any())).thenReturn(new VerifyEmailUseCase.InvalidOrExpired());

        mvc.perform(get("/api/v1/auth/verification/confirm").param("token", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
    }

    @Test
    void missingOrBlankTokenIsTreatedAsInvalid() throws Exception {
        when(verifyEmail.handle(any())).thenReturn(new VerifyEmailUseCase.InvalidOrExpired());

        mvc.perform(get("/api/v1/auth/verification/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
        mvc.perform(get("/api/v1/auth/verification/confirm").param("token", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
    }
}
