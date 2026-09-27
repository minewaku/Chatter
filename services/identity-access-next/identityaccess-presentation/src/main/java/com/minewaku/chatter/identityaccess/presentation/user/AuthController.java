package com.minewaku.chatter.identityaccess.presentation.user;

import com.minewaku.chatter.identityaccess.user.api.command.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase register;

    public AuthController(RegisterUserUseCase r) {
        register = r;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest r) {
        RegisterUserUseCase.Result result =
                register.handle(new RegisterUserUseCase.Command(r.email(), r.username(), r.birthday(), r.password()));
        return result instanceof RegisterUserUseCase.AccountAlreadyActive
                ? ResponseEntity.status(HttpStatus.CONFLICT).build()
                : ResponseEntity.ok().build();
    }
}
