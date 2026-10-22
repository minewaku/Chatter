package com.minewaku.chatter.identityaccess.presentation.shared.error;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that raise every failure the handler must map. Each message carries the word "secret". */
@RestController
class ErrorProbeController {

    record Body(@NotBlank String name) {}

    @PostMapping("/probe/validation")
    String validation(@Valid @RequestBody Body body) {
        return "ok";
    }

    @GetMapping("/probe/illegal-argument")
    String illegalArgument() {
        throw new IllegalArgumentException("secret value 123");
    }

    @GetMapping("/probe/unexpected")
    String unexpected() {
        throw new IllegalStateException("secret internal failure at com.example.Secret");
    }

    @GetMapping("/probe/authentication-failed")
    String authenticationFailed() {
        throw new AuthenticationFailedException();
    }

    @GetMapping("/probe/reauthentication-failed")
    String reauthenticationFailed() {
        throw new ReauthenticationFailedException(new AuthenticationFailedException());
    }

    @GetMapping("/probe/invalid-credentials")
    String invalidCredentials() {
        throw new InvalidCredentialsException("secret current password mismatch");
    }

    @GetMapping("/probe/inaccessible")
    String inaccessible() {
        throw new InvalidAccountStateTransitionException(AccountStatus.SUSPENDED, "secret operation");
    }

    @GetMapping("/probe/account-inaccessible")
    String accountInaccessible() {
        throw new AccountInaccessibleException(new AuthenticationFailedException());
    }

    @GetMapping("/probe/duplicate")
    String duplicate() {
        throw new DuplicateUserAccountException("secret@example.com", new RuntimeException("secret"));
    }

    @GetMapping("/probe/too-many-attempts")
    String tooManyAttempts(@RequestParam(defaultValue = "1500") long millis) {
        throw new TooManyAttemptsException(Duration.ofMillis(millis));
    }

    @GetMapping("/probe/dependency-unavailable")
    String dependencyUnavailable() {
        throw new DependencyUnavailableException(Duration.ofSeconds(10), new RuntimeException("secret store down"));
    }

    @GetMapping("/probe/mail-unavailable")
    String mailUnavailable() {
        throw new MailDeliveryException(new RuntimeException("secret smtp failure for secret@example.com"));
    }
}
