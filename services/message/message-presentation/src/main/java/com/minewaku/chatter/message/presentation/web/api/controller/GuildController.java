package com.minewaku.chatter.message.presentation.web.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.file.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.guild.CreateGuildUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.guild.CreateInviteUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.guild.DeleteGuildUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.guild.UpdateGuildUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.presentation.web.api.request.guild.CreateGuildRequest;
import com.minewaku.chatter.message.presentation.web.api.request.guild.CreateInviteRequest;
import com.minewaku.chatter.message.presentation.web.api.request.guild.UpdateGuildRequest;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Guild Commands", description = "Guild Commands API")
@RestController
@RequestMapping("/api/v1/guilds")
@AllArgsConstructor
public class GuildController {

    private final CreateGuildUseCase createGuildUseCase;
    private final UpdateGuildUseCase updateGuildUseCase;
    private final DeleteGuildUseCase deleteGuildUseCase;
    private final GenerateUploadSignatureUseCase generateUploadSignatureUseCase;
    private final CreateInviteUseCase createInviteUseCase;
    
    @PostMapping("/")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateGuildRequest request) {

        CreateGuildUseCase.Command command = new CreateGuildUseCase.Command(
            new UserId(Long.parseLong(jwt.getSubject())),
            request.name(),
            request.description()
        );
        createGuildUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{guildId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @RequestBody UpdateGuildRequest request) {

        UpdateGuildUseCase.Command command = new UpdateGuildUseCase.Command(
            new GuildId(guildId),
            new UserId(Long.parseLong(jwt.getSubject())),
            request.name(),
            request.description()
        );   
        updateGuildUseCase.handle(command);
            
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{guildId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId) {

        DeleteGuildUseCase.Command command = new DeleteGuildUseCase.Command(
            new GuildId(guildId),
            new UserId(Long.parseLong(jwt.getSubject()))
        );   
        deleteGuildUseCase.handle(command);
            
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{guildId}/icon/signatures")
    public ResponseEntity<AssetStorage.UploadSignature> generateIconUploadSignature(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId) {


        Map<String, Object> params = new HashMap<>(Map.of("userId", jwt.getSubject()));
		Namespace namespace = Namespace.GUILD_ICON;
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(namespace, params);
		generateUploadSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
    }

    @PostMapping("/{guildId}/invites")
    public ResponseEntity<Void> createInvite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @RequestBody CreateInviteRequest request) {

        CreateInviteUseCase.Command command = new CreateInviteUseCase.Command(
            new GuildId(guildId),
            new UserId(Long.parseLong(jwt.getSubject())),
            request.duration(),
            request.maxUses()
        );
        createInviteUseCase.handle(command);
        return ResponseEntity.ok().build();
    }
}
