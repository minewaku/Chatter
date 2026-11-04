package com.minewaku.chatter.profilenext.presentation.image.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.profilenext.presentation.support.TestSecurityConfiguration;
import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase.Outcome;
import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException.Reason;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageUploadController.class)
@Import(TestSecurityConfiguration.class)
class ImageUploadControllerTest {

    private static final Instant EXPIRES = Instant.parse("2026-11-04T10:15:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RequestImageUploadUseCase requestUpload;

    @MockitoBean
    private ConfirmImageUploadUseCase confirmUpload;

    private RequestImageUploadUseCase.Result permission() {
        return new RequestImageUploadUseCase.Result(
                "https://api.cloudinary.com/v1_1/demo/image/upload", "POST", Map.of("signature", "abc"), EXPIRES);
    }

    @Test
    void bothPermissionEndpointsNeedAToken() throws Exception {
        mvc.perform(post("/api/v1/profiles/avatar/upload-signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
        mvc.perform(post("/api/v1/profiles/banner/upload-signature")).andExpect(status().isUnauthorized());
        verifyNoInteractions(requestUpload);
    }

    @Test
    void theAvatarPermissionIsRequestedForTheTokenSubjectOnly() throws Exception {
        when(requestUpload.handle(any())).thenReturn(permission());

        mvc.perform(post("/api/v1/profiles/avatar/upload-signature").with(jwt().jwt(t -> t.subject("123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.fields.signature").value("abc"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-04T10:15:00Z"));

        ArgumentCaptor<RequestImageUploadUseCase.Command> command =
                ArgumentCaptor.forClass(RequestImageUploadUseCase.Command.class);
        verify(requestUpload).handle(command.capture());
        assertThat(command.getValue()).isEqualTo(new RequestImageUploadUseCase.Command(123, "AVATAR"));
    }

    @Test
    void theBannerPermissionUsesTheBannerSlot() throws Exception {
        when(requestUpload.handle(any())).thenReturn(permission());

        mvc.perform(post("/api/v1/profiles/banner/upload-signature").with(jwt().jwt(t -> t.subject("5"))))
                .andExpect(status().isOk());

        ArgumentCaptor<RequestImageUploadUseCase.Command> command =
                ArgumentCaptor.forClass(RequestImageUploadUseCase.Command.class);
        verify(requestUpload).handle(command.capture());
        assertThat(command.getValue()).isEqualTo(new RequestImageUploadUseCase.Command(5, "BANNER"));
    }

    @Test
    void aProviderOutageWhileIssuingAPermissionIsServiceUnavailable() throws Exception {
        when(requestUpload.handle(any())).thenThrow(new ImageStorageUnavailableException("down"));

        mvc.perform(post("/api/v1/profiles/avatar/upload-signature").with(jwt().jwt(t -> t.subject("1"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("service_unavailable"));
    }

    @Test
    void theWebhookNeedsNoTokenAndHandsTheRawBodyAndBothHeadersOverUnchanged() throws Exception {
        when(confirmUpload.handle(any())).thenReturn(new ConfirmImageUploadUseCase.Result(Outcome.APPLIED));
        String body = "{ \"public_id\" :\"café\",   \"x\":1 }";

        mvc.perform(post("/api/v1/profiles/uploads/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Cld-Signature", "sig-1")
                        .header("X-Cld-Timestamp", "1730000000")
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ConfirmImageUploadUseCase.Command> command =
                ArgumentCaptor.forClass(ConfirmImageUploadUseCase.Command.class);
        verify(confirmUpload).handle(command.capture());
        assertThat(command.getValue().rawBody()).isEqualTo(body.getBytes(StandardCharsets.UTF_8));
        assertThat(command.getValue().signature()).isEqualTo("sig-1");
        assertThat(command.getValue().timestamp()).isEqualTo("1730000000");
    }

    @Test
    void anIgnoredRepeatIsStillNoContent() throws Exception {
        when(confirmUpload.handle(any())).thenReturn(new ConfirmImageUploadUseCase.Result(Outcome.IGNORED));

        mvc.perform(post("/api/v1/profiles/uploads/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Cld-Signature", "s")
                        .header("X-Cld-Timestamp", "1")
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aBadSignatureIsUnauthenticated() throws Exception {
        when(confirmUpload.handle(any())).thenThrow(new UploadConfirmationRejectedException(Reason.INVALID_SIGNATURE));

        mvc.perform(post("/api/v1/profiles/uploads/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Cld-Signature", "s")
                        .header("X-Cld-Timestamp", "1")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void aUserMismatchIsForbidden() throws Exception {
        when(confirmUpload.handle(any())).thenThrow(new UploadConfirmationRejectedException(Reason.USER_MISMATCH));

        mvc.perform(post("/api/v1/profiles/uploads/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Cld-Signature", "s")
                        .header("X-Cld-Timestamp", "1")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void aFileOverItsLimitIsForbidden() throws Exception {
        when(confirmUpload.handle(any())).thenReturn(new ConfirmImageUploadUseCase.Result(Outcome.FILE_NOT_ALLOWED));

        mvc.perform(post("/api/v1/profiles/uploads/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Cld-Signature", "s")
                        .header("X-Cld-Timestamp", "1")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }
}
