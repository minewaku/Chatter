package com.minewaku.chatter.identityaccess.presentation.verification.web;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.presentation.verification.dto.ResendVerificationRequest;
import com.minewaku.chatter.identityaccess.presentation.verification.dto.VerificationStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public email verification: request a (new) email and confirm the emailed token. */
@Tag(name = "Email verification", description = "Request a verification email and confirm its token.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/verification")
public class VerificationController {

    private final RequestEmailVerificationUseCase requestVerification;
    private final VerifyEmailUseCase verifyEmail;

    @Operation(
            summary = "Request a verification email (accepted for any well-formed address unless it is asked for too"
                    + " often)")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted, whether or not the address is eligible and whether or not the mail could be sent")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.TOO_MANY_ATTEMPTS,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PostMapping("/resend")
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendVerificationRequest request) {
        requestVerification.handle(new RequestEmailVerificationUseCase.Command(request.email()));
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirm the token from the verification email")
    @ApiResponse(
            responseCode = "200",
            description = "`verified` or `already_verified`",
            content = @Content(schema = @Schema(implementation = VerificationStatusResponse.class)))
    @DocumentedErrors({ErrorCode.INVALID_OR_EXPIRED_TOKEN})
    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam(name = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ApiErrors.response(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        }
        return switch (verifyEmail.handle(new VerifyEmailUseCase.Command(token))) {
            case VerifyEmailUseCase.Verified verified -> ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(VerificationStatusResponse.verified());
            case VerifyEmailUseCase.AlreadyVerified already -> ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(VerificationStatusResponse.alreadyVerified());
            case VerifyEmailUseCase.InvalidOrExpired invalid -> ApiErrors.response(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        };
    }
}
