package com.minewaku.chatter.profilenext.presentation.image.web;

import com.minewaku.chatter.profilenext.presentation.image.dto.UploadPermissionResponse;
import com.minewaku.chatter.profilenext.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.profilenext.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.profilenext.presentation.shared.security.AccessPrincipal;
import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ImageUploadController {

    private final RequestImageUploadUseCase requestUpload;
    private final ConfirmImageUploadUseCase confirmUpload;

    @PostMapping("/avatar/upload-signature")
    public UploadPermissionResponse avatarPermission(@AuthenticationPrincipal Jwt jwt) {
        return permission(jwt, "AVATAR");
    }

    @PostMapping("/banner/upload-signature")
    public UploadPermissionResponse bannerPermission(@AuthenticationPrincipal Jwt jwt) {
        return permission(jwt, "BANNER");
    }

    /**
     * The provider's webhook. Public path: it is authenticated by the signature the use case checks, so the body is
     * taken as raw bytes and reaches the check unchanged.
     */
    @PostMapping("/uploads/notifications")
    public ResponseEntity<?> confirmed(
            @RequestBody byte[] rawBody,
            @RequestHeader("X-Cld-Signature") String signature,
            @RequestHeader("X-Cld-Timestamp") String timestamp) {
        var result = confirmUpload.handle(new ConfirmImageUploadUseCase.Command(rawBody, signature, timestamp));
        if (result.outcome() == ConfirmImageUploadUseCase.Outcome.FILE_NOT_ALLOWED) {
            return ApiErrors.response(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.noContent().build();
    }

    private UploadPermissionResponse permission(Jwt jwt, String slot) {
        return UploadPermissionResponse.from(
                requestUpload.handle(new RequestImageUploadUseCase.Command(AccessPrincipal.from(jwt).userId(), slot)));
    }
}
