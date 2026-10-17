package com.minewaku.chatter.identityaccess.presentation.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.authentication.api.command.AuthenticateUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class AuthControllerTest {

    private static final String REGISTER_BODY =
            """
            {"email":"alice@example.com","username":"alice","birthday":"1999-01-01","password":"S3cure-pass!"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RegisterUserUseCase register;

    @MockitoBean
    AuthenticateUseCase authenticate;

    @MockitoBean
    DeviceLabelResolver deviceLabels;

    @Test
    void registrationReportsTheNewAccountIdentifierOnly() throws Exception {
        when(register.handle(any())).thenReturn(new RegisterUserUseCase.Registered(7L));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("S3cure"))));
    }

    @Test
    void registrationPassesTheSubmittedValuesToTheUseCase() throws Exception {
        when(register.handle(any())).thenReturn(new RegisterUserUseCase.Registered(7L));

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY));

        ArgumentCaptor<RegisterUserUseCase.Command> captor = ArgumentCaptor.forClass(RegisterUserUseCase.Command.class);
        org.mockito.Mockito.verify(register).handle(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().username()).isEqualTo("alice");
        assertThat(captor.getValue().birthday()).hasToString("1999-01-01");
        assertThat(captor.getValue().password()).isEqualTo("S3cure-pass!");
    }

    @Test
    void existingAccountIsAConflict() throws Exception {
        when(register.handle(any())).thenReturn(new RegisterUserUseCase.AccountAlreadyExists());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_already_exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void pendingVerificationIsAConflictWithItsOwnCode() throws Exception {
        when(register.handle(any())).thenReturn(new RegisterUserUseCase.VerificationPending());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("verification_pending"));
    }

    @Test
    void missingOrInvalidFieldsAreValidationFailuresNamingTheFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"S3cure-pass!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(org.hamcrest.Matchers.hasItems("email", "username", "birthday")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("not-an-email"))));
        verifyNoInteractions(register);
    }

    @Test
    void signInReturnsExactlyTheTwoCredentialsWithoutACookie() throws Exception {
        when(deviceLabels.resolve("Mozilla/5.0 Chrome")).thenReturn("Chrome on Linux (Desktop)");
        when(authenticate.handle(any())).thenReturn(new AuthenticateUseCase.Authenticated("access-1", "refresh-1"));

        mvc.perform(post("/api/v1/auth/authenticate")
                        .header("User-Agent", "Mozilla/5.0 Chrome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"S3cure-pass!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-1"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        ArgumentCaptor<AuthenticateUseCase.Command> captor = ArgumentCaptor.forClass(AuthenticateUseCase.Command.class);
        org.mockito.Mockito.verify(authenticate).handle(captor.capture());
        assertThat(captor.getValue().deviceInfo().label()).isEqualTo("Chrome on Linux (Desktop)");
        assertThat(captor.getValue().email()).isEqualTo("alice@example.com");
    }

    @Test
    void failedSignInIsAGenericUnauthenticatedOutcome() throws Exception {
        when(deviceLabels.resolve(any())).thenReturn("Unknown device");
        when(authenticate.handle(any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("wrong"))));
    }
}
