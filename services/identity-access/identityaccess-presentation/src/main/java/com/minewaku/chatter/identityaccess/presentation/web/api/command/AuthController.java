package com.minewaku.chatter.identityaccess.presentation.web.api.command;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LoginUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.VerifyConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.shared.DeviceInfoDto;
import com.minewaku.chatter.identityaccess.presentation.web.request.AuthenticationRequest;
import com.minewaku.chatter.identityaccess.presentation.web.request.ChangePasswordRequest;
import com.minewaku.chatter.identityaccess.presentation.web.request.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

@Tag(name = "Authentication", description = "Authentication API")
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final LoginUseCase loginUseCase;
    private final VerifyConfirmationTokenUseCase verifyConfirmationTokenUseCase;
    private final ResendConfirmationTokenUseCase resendConfirmationTokenUseCase;

    private final UserAgentAnalyzer userAgentAnalyzer;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                request.email(),
                request.username(),
                request.birthday(),
                request.password());

        registerUserUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Authenticate a user and issue tokens")
    @PostMapping("/authenticate")
    public ResponseEntity<String> authentication(
            @RequestBody AuthenticationRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown") String rawUserAgent,
            HttpServletRequest httpRequest) {

        UserAgent parsedAgent = userAgentAnalyzer.parse(rawUserAgent);
        String ipAddress = httpRequest.getRemoteAddr();

        DeviceInfoDto deviceInfo = new DeviceInfoDto(
                ipAddress,
                "Unknown",
                parsedAgent.getValue("DeviceClass"),
                parsedAgent.getValue("OperatingSystemName"),
                parsedAgent.getValue("AgentName"));

        LoginUseCase.Command command = new LoginUseCase.Command(
                request.email(),
                request.password(),
                deviceInfo);

        TokenResponse response = loginUseCase.handle(command);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(30 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response.accessToken());
    }

    @Operation(summary = "Change the current user's password")
    @PostMapping("/@me/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        ChangePasswordUseCase.Command command = new ChangePasswordUseCase.Command(
                Long.parseLong(userId),
                request.password(),
                request.newPassword());
        changePasswordUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Resend the verification email")
    @PostMapping("/verification/resend")
    public ResponseEntity<Void> sendVerifyEmail(@AuthenticationPrincipal Jwt jwt) {
        resendConfirmationTokenUseCase.handle(new ResendConfirmationTokenUseCase.Command(jwt.getClaimAsString("email")));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Confirm a verification token")
    @GetMapping("/verification/confirm")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        verifyConfirmationTokenUseCase.handle(new VerifyConfirmationTokenUseCase.Command(token));
        return ResponseEntity.ok().body("Account verified successfully!");
    }
}
