package com.minewaku.chatter.message.presentation.web.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.channel.CreateChannelUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.channel.DeleteChannelUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.channel.UpdateChannelUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.presentation.web.api.request.channel.CreateChannelRequest;
import com.minewaku.chatter.message.presentation.web.api.request.channel.UpdateChannelRequest;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Channel Commands", description = "Channel Commands API")
@RestController
@RequestMapping("/api/v1/guilds")
@AllArgsConstructor
public class ChannelController {
    
    private final CreateChannelUseCase createChannelUseCase;
    private final UpdateChannelUseCase updateChannelUseCase;
    private final DeleteChannelUseCase deleteChannelUseCase;
    
    @PostMapping("/{guildId}/channels")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateChannelRequest request,
            @PathVariable Long guildId) {

        CreateChannelUseCase.Command command = new CreateChannelUseCase.Command(
            new GuildId(guildId),
            new UserId(Long.parseLong(jwt.getSubject())),
            request.name(),
            request.description()
        );
        createChannelUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{guildId}/channels/{channelId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long channelId,
            @RequestBody UpdateChannelRequest request) {

        UpdateChannelUseCase.Command command = new UpdateChannelUseCase.Command(
            new GuildId(guildId),
            new ChannelId(channelId),
            new UserId(Long.parseLong(jwt.getSubject())),
            request.name(),
            request.description()
        );   
        updateChannelUseCase.handle(command);
            
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{guildId}/channels/{channelId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long channelId) {

        DeleteChannelUseCase.Command command = new DeleteChannelUseCase.Command(
            new GuildId(guildId),
            new ChannelId(channelId),
            new UserId(Long.parseLong(jwt.getSubject()))
        );   
        deleteChannelUseCase.handle(command);
            
        return ResponseEntity.ok().build();
    }
}
