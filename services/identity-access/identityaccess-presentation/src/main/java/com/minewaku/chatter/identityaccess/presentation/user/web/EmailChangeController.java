package com.minewaku.chatter.identityaccess.presentation.user.web;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.presentation.user.dto.EmailChangeStatusResponse;
import com.minewaku.chatter.identityaccess.user.api.command.ConfirmEmailChangeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public confirmation of an email change. The token mailed to the new address is the only proof needed, so the link
 * works from any device and the {@code Authorization} header is ignored.
 */
@Tag(name = "Account", description = "The caller's own account. Requires an access credential.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/email-change")
public class EmailChangeController {

    private final ConfirmEmailChangeUseCase confirmEmailChange;

    @Operation(summary = "Confirm the token mailed to the new address (public, no access credential)")
    @ApiResponse(
            responseCode = "200",
            description = "`confirmed` or `already_confirmed`",
            content = @Content(schema = @Schema(implementation = EmailChangeStatusResponse.class)))
    @DocumentedErrors({ErrorCode.INVALID_OR_EXPIRED_TOKEN, ErrorCode.ACCOUNT_ALREADY_EXISTS})
    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam(name = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ApiErrors.response(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        }
        return switch (confirmEmailChange.handle(new ConfirmEmailChangeUseCase.Command(token))) {
            case ConfirmEmailChangeUseCase.Confirmed confirmed -> ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(EmailChangeStatusResponse.confirmed());
            case ConfirmEmailChangeUseCase.AlreadyConfirmed already -> ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(EmailChangeStatusResponse.alreadyConfirmed());
            case ConfirmEmailChangeUseCase.InvalidOrExpired invalid -> ApiErrors.response(
                    ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        };
    }
}
