package com.minewaku.chatter.identityaccess.presentation.web.api.query;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.identityaccess.application.port.inbound.query.FindSessionsByUserIdUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.query.model.SessionReadModel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Session Read", description = "Session Read API")
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionReadController {

    private final FindSessionsByUserIdUseCase findSessionsByUserIdUseCase;

    public SessionReadController(FindSessionsByUserIdUseCase findSessionsByUserIdUseCase) {
        this.findSessionsByUserIdUseCase = findSessionsByUserIdUseCase;
    }

    @Operation(summary = "List sessions for the current user")
    @GetMapping("")
    public ResponseEntity<Set<SessionReadModel>> logout(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Set<SessionReadModel> sessions = findSessionsByUserIdUseCase.handle(new FindSessionsByUserIdUseCase.Command(Long.parseLong(userId)));
        return ResponseEntity.ok(sessions);
    }
}
