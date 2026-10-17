package com.minewaku.chatter.identityaccess.presentation.authentication;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public credential exchange: renew access with a refresh credential, and sign the presented session out. */
@Tag(name = "Tokens", description = "Renew access and sign out. The refresh credential is the credential.")
@RestController
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final RefreshAccessTokenUseCase refresh;
    private final LogoutCurrentSessionUseCase logout;

    public TokenController(RefreshAccessTokenUseCase refresh, LogoutCurrentSessionUseCase logout) {
        this.refresh = refresh;
        this.logout = logout;
    }

    @Operation(summary = "Exchange a refresh credential for a new access and refresh credential")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.AUTHENTICATION_FAILED,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshAccessTokenUseCase.Refreshed refreshed =
                refresh.handle(new RefreshAccessTokenUseCase.Command(request.refreshToken()));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenPairResponse(refreshed.accessToken(), refreshed.refreshToken()));
    }

    /** Always {@code 204}: the caller cannot tell whether a session existed. */
    @Operation(summary = "Sign out the session of the presented refresh credential")
    @ApiResponse(responseCode = "204", description = "Signed out, or nothing to sign out")
    @DocumentedErrors({ErrorCode.VALIDATION_FAILED, ErrorCode.MALFORMED_REQUEST, ErrorCode.UNSUPPORTED_MEDIA_TYPE})
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logout.handle(new LogoutCurrentSessionUseCase.Command(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }
}
