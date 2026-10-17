package com.minewaku.chatter.identityaccess.presentation.authentication;

import com.minewaku.chatter.identityaccess.emailverification.command.RequestEmailVerificationUseCase;
import com.minewaku.chatter.identityaccess.emailverification.command.VerifyEmailUseCase;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/auth/verification")
public class VerificationController {

    private final RequestEmailVerificationUseCase requestVerification;
    private final VerifyEmailUseCase verifyEmail;

    public VerificationController(RequestEmailVerificationUseCase requestVerification, VerifyEmailUseCase verifyEmail) {
        this.requestVerification = requestVerification;
        this.verifyEmail = verifyEmail;
    }

    @Operation(summary = "Request a verification email (always accepted for a well-formed address)")
    @ApiResponse(responseCode = "202", description = "Accepted, whether or not the address is eligible")
    @DocumentedErrors({ErrorCode.VALIDATION_FAILED, ErrorCode.MALFORMED_REQUEST, ErrorCode.UNSUPPORTED_MEDIA_TYPE})
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
