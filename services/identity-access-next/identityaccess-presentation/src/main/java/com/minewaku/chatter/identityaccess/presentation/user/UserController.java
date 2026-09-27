package com.minewaku.chatter.identityaccess.presentation.user;

import com.minewaku.chatter.identityaccess.user.api.command.SoftDeleteUserAccountUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final SoftDeleteUserAccountUseCase delete;

    public UserController(SoftDeleteUserAccountUseCase d) {
        delete = d;
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        delete.handle(new SoftDeleteUserAccountUseCase.Command(Long.parseLong(jwt.getSubject())));
        return ResponseEntity.ok().build();
    }
}
