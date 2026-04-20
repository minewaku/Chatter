package com.minewaku.chatter.identityaccess.presentation.web.api.command;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LogoutCurrentSessionCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RefreshTokenCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RefreshUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Session Command", description = "Session Command API")
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

	private final RefreshUseCase refreshUseCase;
    private final LogoutCurrentSessionUseCase logoutCurrentSessionUseCase;
	// private final LogoutSpecificSessionUseCase logoutSpecificSessionUseCase;
	// private final LogoutOtherSessionsUseCase logoutOtherSessionsUseCase;

    public SessionController(
			RefreshUseCase refreshUseCase,
            LogoutCurrentSessionUseCase logoutCurrentSessionUseCase
            // LogoutSpecificSessionUseCase logoutSpecificSessionUseCase,
            // LogoutOtherSessionsUseCase logoutOtherSessionsUseCase) {
		){

		this.refreshUseCase = refreshUseCase;
        this.logoutCurrentSessionUseCase = logoutCurrentSessionUseCase;
        // this.logoutSpecificSessionUseCase = logoutSpecificSessionUseCase;
        // this.logoutOtherSessionsUseCase = logoutOtherSessionsUseCase;
    }

	
	@PostMapping("/refresh")
	public ResponseEntity<String> refreshToken(
			@CookieValue(name = "refreshToken", defaultValue = "") String refreshToken) {

		RefreshTokenCommand	command = new RefreshTokenCommand(refreshToken);
		TokenResponse response = refreshUseCase.handle(command);
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


    @DeleteMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = "refreshToken", defaultValue = "") String refreshToken) {

		LogoutCurrentSessionCommand command = new LogoutCurrentSessionCommand(refreshToken);
		logoutCurrentSessionUseCase.handle(command);
		return ResponseEntity.ok().build();
	}



	//required password
	@DeleteMapping("/{sessionId}")
	public ResponseEntity<Void> logoutSpecificSession(
			@PathVariable String sessionId,
			@RequestBody String password,
			@CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) {

		return ResponseEntity.ok().build();
	}

	//required password
	@DeleteMapping("/logout-others")
	public ResponseEntity<Void> logoutOtherSessions(
			@RequestBody String password,
			@CookieValue(name = "refreshToken", defaultValue = "") String refreshToken) {

		return ResponseEntity.ok().build();
	}
}
