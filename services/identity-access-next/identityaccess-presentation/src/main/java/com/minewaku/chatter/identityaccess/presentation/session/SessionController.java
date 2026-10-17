package com.minewaku.chatter.identityaccess.presentation.session;

import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutOtherSessionsUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.command.LogoutSessionUseCase;
import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.presentation.shared.error.AccountInaccessibleException;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ReauthenticationFailedException;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.OpenApiConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.security.AccessPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own sessions. The account and the current session come only from the validated access credential,
 * never from the request.
 */
@Tag(name = "Sessions", description = "The caller's own sessions. Requires an access credential.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final FindSessionsQuery findSessions;
    private final LogoutSessionUseCase logoutSession;
    private final LogoutOtherSessionsUseCase logoutOthers;

    public SessionController(
            FindSessionsQuery findSessions,
            LogoutSessionUseCase logoutSession,
            LogoutOtherSessionsUseCase logoutOthers) {
        this.findSessions = findSessions;
        this.logoutSession = logoutSession;
        this.logoutOthers = logoutOthers;
    }

    @Operation(summary = "List the caller's sessions, newest first")
    @DocumentedErrors({ErrorCode.UNAUTHENTICATED, ErrorCode.ACCOUNT_INACCESSIBLE, ErrorCode.SERVICE_UNAVAILABLE})
    @GetMapping
    public List<SessionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        try {
            return findSessions.handle(new FindSessionsQuery.Command(caller.userId())).stream()
                    .map(SessionResponse::from)
                    .toList();
        } catch (AuthenticationFailedException e) {
            throw new AccountInaccessibleException(e);
        }
    }

    @Operation(summary = "End one of the caller's sessions (requires the current password)")
    @ApiResponse(responseCode = "204", description = "Session ended")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
        ErrorCode.SESSION_NOT_FOUND,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PostMapping("/{sessionId}/revoke")
    public ResponseEntity<?> revoke(
            @PathVariable String sessionId,
            @Valid @RequestBody RevokeSessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        LogoutSessionUseCase.Result result;
        try {
            result = logoutSession.handle(
                    new LogoutSessionUseCase.Command(caller.userId(), sessionId, request.currentPassword()));
        } catch (AuthenticationFailedException e) {
            throw new ReauthenticationFailedException(e);
        }
        return result.revoked() ? ResponseEntity.noContent().build() : ApiErrors.response(ErrorCode.SESSION_NOT_FOUND);
    }

    @Operation(summary = "End every session except the one in use (requires the current password)")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PostMapping("/revoke-others")
    public RevokedCountResponse revokeOthers(
            @Valid @RequestBody RevokeOtherSessionsRequest request, @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        try {
            LogoutOtherSessionsUseCase.Result result = logoutOthers.handle(new LogoutOtherSessionsUseCase.Command(
                    caller.userId(), caller.sessionId(), request.currentPassword()));
            return new RevokedCountResponse(result.revokedCount());
        } catch (AuthenticationFailedException e) {
            throw new ReauthenticationFailedException(e);
        }
    }
}
