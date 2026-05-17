package com.minewaku.chatter.message.presentation.web.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.invite.DeleteInviteUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.invite.JoinGuildByInviteUseCase;
import com.minewaku.chatter.message.domain.model.invite.model.Code;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@RestController
@Tag(name = "Invite Commands", description = "Invite Commands API")
@RequestMapping("/api/v1/invites")
@AllArgsConstructor
public class InviteController {
    
    private final DeleteInviteUseCase deleteInviteUseCase;
    private final JoinGuildByInviteUseCase joinGuildByInviteUseCase;


    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {
                
        DeleteInviteUseCase.Command command = new DeleteInviteUseCase.Command(
            new Code(code),
            new UserId(Long.parseLong(jwt.getSubject()))
        );
        deleteInviteUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<Void> join(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code) {

        JoinGuildByInviteUseCase.Command command = new JoinGuildByInviteUseCase.Command(
            new Code(code),
            new UserId(Long.parseLong(jwt.getSubject()))
        );
        joinGuildByInviteUseCase.handle(command);
        return ResponseEntity.ok().build();
    }
}