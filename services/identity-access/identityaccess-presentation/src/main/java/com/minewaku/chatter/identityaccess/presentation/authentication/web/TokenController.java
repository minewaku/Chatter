package com.minewaku.chatter.identityaccess.presentation.authentication.web;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.command.RefreshAccessTokenUseCase;
import com.minewaku.chatter.identityaccess.presentation.authentication.dto.AccessTokenResponse;
import com.minewaku.chatter.identityaccess.presentation.authentication.support.RefreshCookies;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public credential exchange: renew access with the refresh cookie, and sign the presented session out. */
@Tag(name = "Tokens", description = "Renew access and sign out. The refresh cookie is the credential.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class TokenController {

    private final RefreshAccessTokenUseCase refresh;
    private final LogoutCurrentSessionUseCase logout;
    private final RefreshCookies refreshCookies;

    @Operation(
            summary = "Exchange the refresh cookie for a new access credential and a new refresh cookie",
            description = "The presented refresh cookie is replaced; reusing it afterwards ends the session.")
    @Parameter(
            name = "refresh_token",
            in = ParameterIn.COOKIE,
            description = "The refresh credential cookie set at sign-in. Absent or blank gives `authentication_failed`.")
    @DocumentedErrors({ErrorCode.AUTHENTICATION_FAILED})
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        RefreshAccessTokenUseCase.Refreshed refreshed =
                refresh.handle(new RefreshAccessTokenUseCase.Command(refreshCookies.read(request)));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookies.issue(refreshed.refreshToken()))
                .body(new AccessTokenResponse(refreshed.accessToken()));
    }

    /** Always {@code 204}: the caller cannot tell whether a session existed. The cookie is cleared either way. */
    @Operation(summary = "Sign out the session of the refresh cookie and clear it")
    @Parameter(
            name = "refresh_token",
            in = ParameterIn.COOKIE,
            description = "The refresh credential cookie set at sign-in. Absent or blank revokes nothing.")
    @ApiResponse(responseCode = "204", description = "Signed out, or nothing to sign out")
    @DocumentedErrors({})
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        logout.handle(new LogoutCurrentSessionUseCase.Command(refreshCookies.read(request)));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear())
                .build();
    }
}
