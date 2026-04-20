package com.minewaku.chatter.identityaccess.presentation.web.api.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.SoftDeleteUserUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Command", description = "User Command API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final SoftDeleteUserUseCase softDeleteUserUseCase;

    public UserController(
                SoftDeleteUserUseCase softDeleteUserUseCase) {

        this.softDeleteUserUseCase = softDeleteUserUseCase;
    }
    
    @DeleteMapping("")
    public ResponseEntity<Void> deleteUser(
                @AuthenticationPrincipal Jwt jwt) {
                    
        String userId = jwt.getSubject();
        softDeleteUserUseCase.handle(new UserId(Long.parseLong(userId)));
        return ResponseEntity.ok().build();
    }
}
