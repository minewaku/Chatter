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

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.ChangePasswordCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LoginCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RegisterCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LoginUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.VerifyConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Birthday;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Username;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.presentation.web.request.AuthenticationRequest;
import com.minewaku.chatter.identityaccess.presentation.web.request.ChangePasswordRequest;
import com.minewaku.chatter.identityaccess.presentation.web.request.RegisterRequest;

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

	@PostMapping("/register")
	public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
		RegisterCommand command = new RegisterCommand(
				new Email(request.email()),
				new Username(request.username()),
				new Birthday(request.birthday()),
				new Password(request.password()));

		registerUserUseCase.handle(command);
		return ResponseEntity.ok().build();
	}

    @PostMapping("/authenticate")
    public ResponseEntity<String> authentication(
                @RequestBody AuthenticationRequest request,
                @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown") String rawUserAgent,
				HttpServletRequest httpRequest) {


        UserAgent parsedAgent = userAgentAnalyzer.parse(rawUserAgent);
        String ipAddress = httpRequest.getRemoteAddr();

		DeviceInfo deviceInfo = DeviceInfo.builder()
                .ipAddress(ipAddress)
                .country("Unknown")
                .rawUserAgent(rawUserAgent)
                .deviceType(parsedAgent.getValue("DeviceClass"))
                .deviceBrand(parsedAgent.getValue("DeviceBrand"))
                .osName(parsedAgent.getValue("OperatingSystemName"))
                .osVersion(parsedAgent.getValue("OperatingSystemVersion"))
                .browserName(parsedAgent.getValue("AgentName"))
                .browserVersion(parsedAgent.getValue("AgentVersion"))
                .build();

        LoginCommand command = new LoginCommand(
                new Email(request.email()),
                new Password(request.password()),
                deviceInfo
        );

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

	@PostMapping("/@me/change-password")
	public ResponseEntity<Void> changePassword(
			@RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal Jwt jwt) {

		String userId = jwt.getSubject();
		ChangePasswordCommand command = new ChangePasswordCommand(
				new UserId(Long.parseLong(userId)),
				new Password(request.password()),
				new Password(request.newPassword()));
		changePasswordUseCase.handle(command);
		return ResponseEntity.ok().build();
	}



	@PostMapping("/verification/resend")
	public ResponseEntity<Void> sendVerifyEmail(
				@AuthenticationPrincipal Jwt jwt) {

		Email email = new Email(jwt.getClaimAsString("email"));
		resendConfirmationTokenUseCase.handle(email);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/verification/confirm")
	public ResponseEntity<String> verifyEmail(@RequestParam String token) {
		verifyConfirmationTokenUseCase.handle(token);
		return ResponseEntity.ok().body("Account verified successfully!");
	}
}
