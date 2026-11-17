package com.minewaku.chatter.profilenext.presentation.image.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.profilenext.presentation.image.ImageUrlResolver;
import com.minewaku.chatter.profilenext.presentation.support.TestSecurityConfiguration;
import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.RemoveImageUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadTicketUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageUploadRejectedException.Reason;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageUploadController.class)
@Import({TestSecurityConfiguration.class, ImageUrlResolver.class})
@TestPropertySource(properties = "profile-next.images.public-base-url=https://img.test")
class ImageUploadControllerTest {

    private static final Instant EXPIRES = Instant.parse("2026-11-04T10:05:00Z");
    private static final String IMAGE_ID = "0197aaab-0000-7000-8000-000000000001";
    private static final String BODY = "{\"contentType\":\"image/png\",\"sizeBytes\":1234}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RequestImageUploadTicketUseCase requestUpload;

    @MockitoBean
    private CommitImageUploadUseCase commitUpload;

    @MockitoBean
    private RemoveImageUseCase removeImage;

    private RequestImageUploadTicketUseCase.Result ticket() {
        return new RequestImageUploadTicketUseCase.Result(
                IMAGE_ID,
                "https://storage.test/profiles/123/avatar/" + IMAGE_ID + "?sig",
                "PUT",
                Map.of("Content-Type", "image/png"),
                Map.of(),
                EXPIRES);
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mvc.perform(post("/api/v1/profiles/@me/images/avatar/upload").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(post("/api/v1/profiles/@me/images/avatar/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageId\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/profiles/@me/images/avatar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(requestUpload, commitUpload, removeImage);
    }

    @Test
    void aTicketIsRequestedForTheTokenSubjectOnly() throws Exception {
        when(requestUpload.handle(any())).thenReturn(ticket());

        mvc.perform(post("/api/v1/profiles/@me/images/avatar/upload")
                        .with(jwt().jwt(t -> t.subject("123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId").value(IMAGE_ID))
                .andExpect(jsonPath("$.uploadUrl").value("https://storage.test/profiles/123/avatar/" + IMAGE_ID + "?sig"))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.headers['Content-Type']").value("image/png"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-04T10:05:00Z"));

        ArgumentCaptor<RequestImageUploadTicketUseCase.Command> command =
                ArgumentCaptor.forClass(RequestImageUploadTicketUseCase.Command.class);
        verify(requestUpload).handle(command.capture());
        assertThat(command.getValue()).isEqualTo(new RequestImageUploadTicketUseCase.Command(123, "AVATAR", "image/png", 1234));
    }

    @Test
    void theBannerUsesTheBannerSlot() throws Exception {
        when(requestUpload.handle(any())).thenReturn(ticket());

        mvc.perform(post("/api/v1/profiles/@me/images/banner/upload")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<RequestImageUploadTicketUseCase.Command> command =
                ArgumentCaptor.forClass(RequestImageUploadTicketUseCase.Command.class);
        verify(requestUpload).handle(command.capture());
        assertThat(command.getValue().slot()).isEqualTo("BANNER");
        assertThat(command.getValue().userId()).isEqualTo(5);
    }

    @Test
    void anUnknownSlotIsNotFoundAndReachesNoUseCase() throws Exception {
        mvc.perform(post("/api/v1/profiles/@me/images/cover/upload")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound());
        verifyNoInteractions(requestUpload);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "{}", "{\"contentType\":\"\",\"sizeBytes\":10}", "{\"contentType\":\"image/png\"}",
                "{\"contentType\":\"image/png\",\"sizeBytes\":0}", "{\"contentType\":\"image/png\",\"sizeBytes\":-4}"
            })
    void aBadTicketRequestIsAValidationFailure(String body) throws Exception {
        mvc.perform(post("/api/v1/profiles/@me/images/avatar/upload")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verifyNoInteractions(requestUpload);
    }

    @Test
    void aCommitReturnsTheImageAddress() throws Exception {
        when(commitUpload.handle(any()))
                .thenReturn(new CommitImageUploadUseCase.Result(
                        CommitImageUploadUseCase.Outcome.APPLIED, IMAGE_ID, "profiles/123/avatar/" + IMAGE_ID));

        mvc.perform(post("/api/v1/profiles/@me/images/avatar/commit")
                        .with(jwt().jwt(t -> t.subject("123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageId\":\"" + IMAGE_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slot").value("avatar"))
                .andExpect(jsonPath("$.outcome").value("applied"))
                .andExpect(jsonPath("$.imageId").value(IMAGE_ID))
                .andExpect(jsonPath("$.imageUrl").value("https://img.test/profiles/123/avatar/" + IMAGE_ID));

        ArgumentCaptor<CommitImageUploadUseCase.Command> command =
                ArgumentCaptor.forClass(CommitImageUploadUseCase.Command.class);
        verify(commitUpload).handle(command.capture());
        assertThat(command.getValue()).isEqualTo(new CommitImageUploadUseCase.Command(123, "AVATAR", IMAGE_ID));
    }

    @Test
    void aRepeatedCommitIsReportedAsAlreadyApplied() throws Exception {
        when(commitUpload.handle(any()))
                .thenReturn(new CommitImageUploadUseCase.Result(
                        CommitImageUploadUseCase.Outcome.ALREADY_APPLIED, IMAGE_ID, "profiles/5/banner/" + IMAGE_ID));

        mvc.perform(post("/api/v1/profiles/@me/images/banner/commit")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageId\":\"" + IMAGE_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("already_applied"));
    }

    @Test
    void aCommitWithoutAnImageIdIsAValidationFailure() throws Exception {
        mvc.perform(post("/api/v1/profiles/@me/images/avatar/commit")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verifyNoInteractions(commitUpload);
    }

    @ParameterizedTest
    @CsvSource({
        "FORMAT_NOT_ALLOWED,422,image_rejected",
        "FILE_TOO_LARGE,422,image_rejected",
        "DIMENSIONS_NOT_ALLOWED,422,image_rejected",
        "UNREADABLE_IMAGE,422,image_rejected",
        "CONTENT_TYPE_MISMATCH,422,image_rejected",
        "UPLOAD_NOT_FOUND,404,not_found",
        "UPLOAD_EXPIRED,410,upload_expired",
        "UPLOAD_INCOMPLETE,409,upload_incomplete",
        "UPLOAD_CONFLICT,409,conflict"
    })
    void eachRefusalHasItsOwnStatusAndCode(Reason reason, int status, String code) throws Exception {
        when(commitUpload.handle(any())).thenThrow(new ImageUploadRejectedException(reason));

        mvc.perform(post("/api/v1/profiles/@me/images/avatar/commit")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageId\":\"" + IMAGE_ID + "\"}"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void aRefusedTicketRequestIsUnprocessableAndNamesTheRuleInAFixedSentence() throws Exception {
        when(requestUpload.handle(any())).thenThrow(new ImageUploadRejectedException(Reason.FILE_TOO_LARGE));

        mvc.perform(post("/api/v1/profiles/@me/images/avatar/upload")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field").value("image"))
                .andExpect(jsonPath("$.errors[0].message").value("The file is larger than this image allows."));
    }

    @Test
    void aStorageOutageIsServiceUnavailable() throws Exception {
        when(requestUpload.handle(any())).thenThrow(new ImageStorageUnavailableException("down"));

        mvc.perform(post("/api/v1/profiles/@me/images/avatar/upload")
                        .with(jwt().jwt(t -> t.subject("5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("service_unavailable"));
    }

    @Test
    void removingAnImageAnswersNoContentForTheTokenSubject() throws Exception {
        mvc.perform(delete("/api/v1/profiles/@me/images/banner").with(jwt().jwt(t -> t.subject("9"))))
                .andExpect(status().isNoContent());

        ArgumentCaptor<RemoveImageUseCase.Command> command = ArgumentCaptor.forClass(RemoveImageUseCase.Command.class);
        verify(removeImage).handle(command.capture());
        assertThat(command.getValue()).isEqualTo(new RemoveImageUseCase.Command(9, "BANNER"));
    }
}
