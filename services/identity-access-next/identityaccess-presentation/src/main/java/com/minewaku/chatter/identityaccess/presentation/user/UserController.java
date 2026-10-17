package com.minewaku.chatter.identityaccess.presentation.user;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.OpenApiConfiguration;
import com.minewaku.chatter.identityaccess.presentation.shared.security.AccessPrincipal;
import com.minewaku.chatter.identityaccess.user.api.command.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    public UserController(ChangePasswordUseCase changePassword, SoftDeleteUserAccountUseCase softDelete) {
        this.changePassword = changePassword;
        this.softDelete = softDelete;
    }

    /** Changing the password ends all of the account's sessions, including the one in use. */
    @Operation(summary = "Change the caller's password and end all of their sessions")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.UNAUTHENTICATED,
        ErrorCode.REAUTHENTICATION_FAILED,
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

    /** Idempotent: closing an already closed account is also {@code 204}. */
    @Operation(summary = "Close the caller's account")
    @ApiResponse(responseCode = "204", description = "Account closed (also when it already was)")
    @DocumentedErrors({ErrorCode.UNAUTHENTICATED, ErrorCode.SERVICE_UNAVAILABLE})
    @DeleteMapping
    public ResponseEntity<Void> close(@AuthenticationPrincipal Jwt jwt) {
        AccessPrincipal caller = AccessPrincipal.from(jwt);
        softDelete.handle(new SoftDeleteUserAccountUseCase.Command(caller.userId()));
        return ResponseEntity.noContent().build();
    }
}
