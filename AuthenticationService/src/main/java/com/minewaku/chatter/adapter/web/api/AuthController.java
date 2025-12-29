package com.minewaku.chatter.adapter.web.api;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.adapter.web.request.AuthenticationRequest;
import com.minewaku.chatter.adapter.web.request.ChangePasswordRequest;
import com.minewaku.chatter.adapter.web.request.RegisterRequest;
import com.minewaku.chatter.adapter.web.request.SendVerifyEmailRequest;
import com.minewaku.chatter.adapter.web.response.AuthTokenResponse;
import com.minewaku.chatter.application.service.auth.ChangePasswordApplicationService;
import com.minewaku.chatter.application.service.auth.LoginApplicationService;
import com.minewaku.chatter.application.service.auth.LogoutApplicationService;
import com.minewaku.chatter.application.service.auth.RefreshApplicationService;
import com.minewaku.chatter.application.service.auth.RegisterApplicationService;
import com.minewaku.chatter.application.service.auth.ResendConfirmationTokenApplicationService;
import com.minewaku.chatter.application.service.auth.VerifyConfirmationTokenApplicationService;
import com.minewaku.chatter.domain.command.auth.ChangePasswordCommand;
import com.minewaku.chatter.domain.command.auth.LoginCommand;
import com.minewaku.chatter.domain.command.auth.RegisterCommand;
import com.minewaku.chatter.domain.response.TokenResponse;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Password;
import com.minewaku.chatter.domain.value.Username;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "Authentication API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final RegisterApplicationService registerApplicationService;
	private final ChangePasswordApplicationService changePasswordApplicationService;
	private final LoginApplicationService loginApplicationService;
	private final LogoutApplicationService logoutApplicationService;
	private final RefreshApplicationService refreshApplicationService;
	private final VerifyConfirmationTokenApplicationService verifyConfirmationTokenApplicationService;
	private final ResendConfirmationTokenApplicationService resendConfirmationTokenApplicationService;

	public AuthController(
			RegisterApplicationService registerApplicationService,
			ChangePasswordApplicationService changePasswordApplicationService,
			LoginApplicationService loginApplicationService,
			LogoutApplicationService logoutApplicationService,
			RefreshApplicationService refreshApplicationService,
			VerifyConfirmationTokenApplicationService verifyConfirmationTokenApplicationService,
			ResendConfirmationTokenApplicationService resendConfirmationTokenApplicationService) {
		this.registerApplicationService = registerApplicationService;
		this.changePasswordApplicationService = changePasswordApplicationService;
		this.loginApplicationService = loginApplicationService;
		this.logoutApplicationService = logoutApplicationService;
		this.refreshApplicationService = refreshApplicationService;
		this.verifyConfirmationTokenApplicationService = verifyConfirmationTokenApplicationService;
		this.resendConfirmationTokenApplicationService = resendConfirmationTokenApplicationService;
	}

	@PostMapping("/register")
	@Transactional
	public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
		RegisterCommand command = new RegisterCommand(
				new Email(request.email()),
				new Username(request.username()),
				new Birthday(request.birthday()),
				new Password(request.password()));

		registerApplicationService.handle(command);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/authenticate")
	public ResponseEntity<AuthTokenResponse> authentication(@RequestBody AuthenticationRequest request) {
		LoginCommand command = new LoginCommand(
				new Email(request.email()),
				new Password(request.password()));
		TokenResponse domainResponse = loginApplicationService.handle(command);
		AuthTokenResponse response = new AuthTokenResponse(domainResponse.accessToken(),
				domainResponse.refreshToken().getToken());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> resetPassword(@RequestBody ChangePasswordRequest request) {
		ChangePasswordCommand command = new ChangePasswordCommand(new Email(request.email()),
				new Password(request.password()),
				new Password(request.newPassword()));
		changePasswordApplicationService.handle(command);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthTokenResponse> refreshToken(
			@CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) {
		TokenResponse domainResponse = refreshApplicationService.handle(refreshToken);
		AuthTokenResponse response = new AuthTokenResponse(domainResponse.accessToken(),
				domainResponse.refreshToken().getToken());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) {
		logoutApplicationService.handle(refreshToken);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/verification/resend")
	public ResponseEntity<Void> sendVerifyEmail(@RequestBody SendVerifyEmailRequest request) {
		Email email = new Email(request.email());
		resendConfirmationTokenApplicationService.handle(email);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/verification/confirm")
	public ResponseEntity<String> verifyEmail(@RequestParam String token) {
		verifyConfirmationTokenApplicationService.handle(token);
		return ResponseEntity.ok().body("Account verified successfully!");
	}
}
