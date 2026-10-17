package com.minewaku.chatter.identityaccess.presentation.authentication;

import com.minewaku.chatter.identityaccess.authentication.api.command.AuthenticateUseCase;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiErrors;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import com.minewaku.chatter.identityaccess.presentation.shared.openapi.DocumentedErrors;
import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public account entry points: registration and sign-in. */
@Tag(name = "Authentication", description = "Registration and sign-in. No credential required.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase register;
    private final AuthenticateUseCase authenticate;
    private final DeviceLabelResolver deviceLabels;

    public AuthController(
            RegisterUserUseCase register, AuthenticateUseCase authenticate, DeviceLabelResolver deviceLabels) {
        this.register = register;
        this.authenticate = authenticate;
        this.deviceLabels = deviceLabels;
    }

    @Operation(summary = "Register a new account")
    @ApiResponse(
            responseCode = "201",
            description = "Account created; a verification email is requested",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class)))
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.ACCOUNT_ALREADY_EXISTS,
        ErrorCode.VERIFICATION_PENDING,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserUseCase.Result result = register.handle(new RegisterUserUseCase.Command(
                request.email(), request.username(), request.birthday(), request.password()));
        return switch (result) {
            case RegisterUserUseCase.Registered registered -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse(registered.userId()));
            case RegisterUserUseCase.AccountAlreadyExists exists -> ApiErrors.response(
                    ErrorCode.ACCOUNT_ALREADY_EXISTS);
            case RegisterUserUseCase.VerificationPending pending -> ApiErrors.response(ErrorCode.VERIFICATION_PENDING);
        };
    }

    @Operation(summary = "Sign in and receive an access and a refresh credential")
    @DocumentedErrors({
        ErrorCode.VALIDATION_FAILED,
        ErrorCode.MALFORMED_REQUEST,
        ErrorCode.AUTHENTICATION_FAILED,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE
    })
    @PostMapping("/authenticate")
    public ResponseEntity<TokenPairResponse> authenticate(
            @Valid @RequestBody AuthenticateRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        AuthenticateUseCase.Authenticated authenticated = authenticate.handle(new AuthenticateUseCase.Command(
                request.email(),
                request.password(),
                new AuthenticateUseCase.DeviceInfo(deviceLabels.resolve(userAgent))));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenPairResponse(authenticated.accessToken(), authenticated.refreshToken()));
    }
}
