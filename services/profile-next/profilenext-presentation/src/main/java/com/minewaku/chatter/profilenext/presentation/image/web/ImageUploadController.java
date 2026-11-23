package com.minewaku.chatter.profilenext.presentation.image.web;

import com.minewaku.chatter.profilenext.presentation.image.ImageUrlResolver;
import com.minewaku.chatter.profilenext.presentation.image.dto.CommitUploadRequest;
import com.minewaku.chatter.profilenext.presentation.image.dto.CommitUploadResponse;
import com.minewaku.chatter.profilenext.presentation.image.dto.UploadPermissionRequest;
import com.minewaku.chatter.profilenext.presentation.image.dto.UploadPermissionResponse;
import com.minewaku.chatter.profilenext.presentation.shared.security.AccessPrincipal;
import com.minewaku.chatter.profilenext.profile.api.command.CommitImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.RemoveImageUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadTicketUseCase;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three steps of an image change, for the caller only (the token's subject; no path or body field can name another
 * user). The file itself never passes through here: the client uploads it straight to the storage with the ticket from
 * {@code upload}, then calls {@code commit} with the ticket's image id; the ticket already knows its slot. Any slot other
 * than {@code avatar} or {@code banner} does not match a route.
 */
@RestController
@RequestMapping("/api/v1/profiles/@me/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final RequestImageUploadTicketUseCase requestUpload;
    private final CommitImageUploadUseCase commitUpload;
    private final RemoveImageUseCase removeImage;
    private final ImageUrlResolver urls;

    @PostMapping("/{slot:avatar|banner}/upload")
    public UploadPermissionResponse upload(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String slot, @Valid @RequestBody UploadPermissionRequest request) {
        return UploadPermissionResponse.from(requestUpload.handle(new RequestImageUploadTicketUseCase.Command(
                AccessPrincipal.from(jwt).userId(), slotName(slot), request.contentType(), request.sizeBytes())));
    }

    @PostMapping("/commit")
    public CommitUploadResponse commit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CommitUploadRequest request) {
        var result =
                commitUpload.handle(new CommitImageUploadUseCase.Command(AccessPrincipal.from(jwt).userId(), request.imageId()));
        return CommitUploadResponse.from(result, urls.resolve(result.path()));
    }

    @DeleteMapping("/{slot:avatar|banner}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String slot) {
        removeImage.handle(new RemoveImageUseCase.Command(AccessPrincipal.from(jwt).userId(), slotName(slot)));
    }

    private static String slotName(String slot) {
        return slot.toUpperCase(Locale.ROOT);
    }
}
