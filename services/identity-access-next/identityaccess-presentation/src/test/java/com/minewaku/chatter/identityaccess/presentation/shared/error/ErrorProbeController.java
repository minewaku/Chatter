package com.minewaku.chatter.identityaccess.presentation.shared.error;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
