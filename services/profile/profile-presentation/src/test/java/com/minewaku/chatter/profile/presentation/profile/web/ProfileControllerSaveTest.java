package com.minewaku.chatter.profile.presentation.profile.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.profile.presentation.support.TestSecurityConfiguration;
import com.minewaku.chatter.profile.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profile.profile.api.exception.InvalidProfileValueException;
import com.minewaku.chatter.profile.profile.api.query.GetProfileUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ProfileController.class)
@Import(TestSecurityConfiguration.class)
class ProfileControllerSaveTest {

    @MockitoBean
    private com.minewaku.chatter.profile.presentation.image.ImageUrlResolver urls;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetProfileUseCase getProfile;

    @MockitoBean
    private SaveProfileUseCase saveProfile;

    private static RequestPostProcessor caller(long userId) {
        return jwt().jwt(token -> token.subject(Long.toString(userId)));
    }

    private SaveProfileUseCase.Command submitted() {
        ArgumentCaptor<SaveProfileUseCase.Command> captor = ArgumentCaptor.forClass(SaveProfileUseCase.Command.class);
        verify(saveProfile).handle(captor.capture());
        return captor.getValue();
    }

    @Test
    void aNameOnlySaveReturnsNoContentAndPassesNoBio() throws Exception {
        when(saveProfile.handle(any())).thenReturn(new SaveProfileUseCase.Result(true));

        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Mine\"}"))
                .andExpect(status().isNoContent());

        SaveProfileUseCase.Command command = submitted();
        assertThat(command.userId()).isEqualTo(123);
        assertThat(command.displayName()).isEqualTo("Mine");
        assertThat(command.bio()).isNull();
    }

    @Test
    void aBioOnlySavePassesNoName() throws Exception {
        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Hello\"}"))
                .andExpect(status().isNoContent());

        SaveProfileUseCase.Command command = submitted();
        assertThat(command.displayName()).isNull();
        assertThat(command.bio()).isEqualTo("Hello");
    }

    @Test
    void anEmptyObjectSucceedsAndThePartsAreBothAbsent() throws Exception {
        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        SaveProfileUseCase.Command command = submitted();
        assertThat(command.displayName()).isNull();
        assertThat(command.bio()).isNull();
    }

    @Test
    void aRuleFailureIsAValidationFailureNamingTheFieldAndNotEchoingTheValue() throws Exception {
        when(saveProfile.handle(any()))
                .thenThrow(new InvalidProfileValueException(
                        "displayName", "display_name_blank", "Display name must be 1 to 32 characters."));

        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("displayName"))
                .andExpect(jsonPath("$.errors[0].message").value("Display name must be 1 to 32 characters."));
    }

    @Test
    void malformedJsonIsAMalformedRequest() throws Exception {
        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"));
        verifyNoInteractions(saveProfile);
    }

    @Test
    void aUserIdInTheBodyIsIgnoredAndTheTokenSubjectWins() throws Exception {
        mvc.perform(patch("/api/v1/profiles/@me")
                        .with(caller(123))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Mine\",\"userId\":999,\"user_id\":999}"))
                .andExpect(status().isNoContent());

        assertThat(submitted().userId()).isEqualTo(123);
    }

    @Test
    void noTokenIsUnauthenticated() throws Exception {
        mvc.perform(patch("/api/v1/profiles/@me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Mine\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        verifyNoInteractions(saveProfile);
    }
}
