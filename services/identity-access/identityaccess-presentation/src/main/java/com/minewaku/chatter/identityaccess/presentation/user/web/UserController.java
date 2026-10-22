package com.minewaku.chatter.identityaccess.presentation.user.web;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.OpenApiConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.security.AccessPrincipal;
import com.minewaku.chatter.identityaccess.presentation.user.dto.ChangeEmailRequest;
import com.minewaku.chatter.identityaccess.presentation.user.dto.ChangePasswordRequest;
import com.minewaku.chatter.identityaccess.presentation.user.dto.ChangeUsernameRequest;
import com.minewaku.chatter.identityaccess.presentation.user.dto.CloseAccountRequest;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.ChangeUsernameUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.RequestEmailChangeUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own account. The target is always the account named by the validated access credential. */
@Tag(name = "Account", description = "The caller's own account. Requires an access credential.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME)
@RestController
@RequestMapping("/api/v1/users/@me")
public class UserController {

    private final ChangePasswordUseCase changePassword;
    private final SoftDeleteUserAccountUseCase softDelete;
    private final ChangeUsernameUseCase changeUsername;
    private final RequestEmailChangeUseCase requestEmailChange;

    public UserController(
            ChangePasswordUseCase changePassword,
            SoftDeleteUserAccountUseCase softDelete,
            ChangeUsernameUseCase changeUsername,
            RequestEmailChangeUseCase requestEmailChange) {
        this.changePassword = changePassword;
        this.softDelete = softDelete;
        this.changeUsername = changeUsername;
        this.requestEmailChange = requestEmailChange;
    }

    /** Changing the password ends all of the account's sessions, including the one in use. */
    @Operation(summary = "Change the caller's password and end all of their sessions")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
        ErrorCode.TOO_MANY_ATTEMPTS,
        ErrorCode.ACCOUNT_INACCESSIBLE,
        ErrorCode.PASSWORD_UNCHANGED,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        ChangePasswordUseCase.Result result = changePassword.handle(
                new ChangePasswordUseCase.Command(caller.userId(), request.currentPassword(), request.newPassword()));
        return result.changed() ? ResponseEntity.noContent().build() : ApiErrors.response(ErrorCode.PASSWORD_UNCHANGED);
    }

    /** A username is a display name, so changing it needs no password and ends no session. */
    @Operation(summary = "Change the caller's username")
    @ApiResponse(responseCode = "204", description = "Username changed")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.ACCOUNT_INACCESSIBLE,
        ErrorCode.USERNAME_UNCHANGED,
        ErrorCode.ACCOUNT_ALREADY_EXISTS,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PutMapping("/username")
    public ResponseEntity<?> changeUsername(
            @Valid @RequestBody ChangeUsernameRequest request, @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        ChangeUsernameUseCase.Result result =
                changeUsername.handle(new ChangeUsernameUseCase.Command(caller.userId(), request.username()));
        return result.changed() ? ResponseEntity.noContent().build() : ApiErrors.response(ErrorCode.USERNAME_UNCHANGED);
    }

    /**
     * Only starts the change: the account keeps its email, and the new address gets a token to confirm. A wrong
     * password is refused and leaves the session alone.
     */
    @Operation(
            summary = "Ask to change the caller's email (requires the current password; the new address must confirm)")
    @ApiResponse(responseCode = "202", description = "Accepted: a confirmation was mailed to the new address")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
        ErrorCode.TOO_MANY_ATTEMPTS,
        ErrorCode.ACCOUNT_INACCESSIBLE,
        ErrorCode.EMAIL_UNCHANGED,
        ErrorCode.ACCOUNT_ALREADY_EXISTS,
        ErrorCode.SERVICE_UNAVAILABLE,
        ErrorCode.MAIL_UNAVAILABLE
    })
    @PostMapping("/email-change")
    public ResponseEntity<?> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequest request, @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        RequestEmailChangeUseCase.Result result = requestEmailChange.handle(
                new RequestEmailChangeUseCase.Command(caller.userId(), request.currentPassword(), request.newEmail()));
        return result.requested() ? ResponseEntity.accepted().build() : ApiErrors.response(ErrorCode.EMAIL_UNCHANGED);
    }

    /** Closing ends all of the account's sessions. A wrong password is refused and leaves the session alone. */
    @Operation(summary = "Close the caller's account (requires the current password)")
    @ApiResponse(responseCode = "204", description = "Account closed")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
        ErrorCode.TOO_MANY_ATTEMPTS,
        ErrorCode.SERVICE_UNAVAILABLE
    })
    @PostMapping("/close")
    public ResponseEntity<Void> close(
            @Valid @RequestBody CloseAccountRequest request, @AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        softDelete.handle(new SoftDeleteUserAccountUseCase.Command(caller.userId(), request.currentPassword()));
        return ResponseEntity.noContent().build();
    }
}
