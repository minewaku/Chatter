package com.minewaku.chatter.message.presentation.web.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.recipient.RemoveRecipientUseCase;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Guild Commands", description = "Guild Commands API")
@RestController
@RequestMapping("/api/v1/guilds")
@AllArgsConstructor
public class RecipientController {
    
    private final RemoveRecipientUseCase removeRecipientUseCase;

    @DeleteMapping("/{guildId}/recipients/{recipientId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long recipientId) {

        RemoveRecipientUseCase.Command command = new RemoveRecipientUseCase.Command(
            new GuildId(guildId),
            new UserId(recipientId),
            new UserId(Long.parseLong(jwt.getSubject()))
        );   
        removeRecipientUseCase.handle(command);
            
        return ResponseEntity.ok().build();
    }

}
