package com.minewaku.chatter.message.presentation.web.api.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.asset.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.message.DeleteMessageUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.message.SendMessageUseCase;
import com.minewaku.chatter.message.application.port.inbound.query.GetChannelMessagesUseCase;
import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.presentation.web.api.request.message.CreateMessageRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;


@RestController
@Tag(name = "Message Commands", description = "Message Commands API")
@RequestMapping("/api/v1/guilds")
@AllArgsConstructor
public class MessageController {
    
    private final SendMessageUseCase sendMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;
    private final GetChannelMessagesUseCase getChannelMessagesUseCase;

    @Qualifier("UploadAttachmentNotificationUseCase")
    private final GenerateUploadSignatureUseCase generateUploadAttachmentSignatureUseCase;

    @PostMapping("/{guildId}/channels/{channelId}/messages")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long channelId,
            @RequestBody CreateMessageRequest request
        ) {

        SendMessageUseCase.Command command = new SendMessageUseCase.Command(
            new GuildId(guildId),
            new ChannelId(channelId),
            new UserId(Long.parseLong(jwt.getSubject())),
            request.replyId() != null ? new MessageId(request.replyId()) : null,
            request.content(),
            new ArrayList<>()
        );
        sendMessageUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{guildId}/channels/{channelId}/messages/{messageId}/attachments/signatures")
    public ResponseEntity<AssetStorage.UploadSignature> generateIconUploadSignature(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long channelId,
            @PathVariable Long messageId
    ) {
        Map<String, Object> params = new HashMap<>(Map.of("userId", jwt.getSubject()));
        params.put("guildId", String.valueOf(guildId));
        params.put("channelId", String.valueOf(channelId));
        params.put("messageId", String.valueOf(messageId));
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(
            params
        );
		generateUploadAttachmentSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadAttachmentSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
    }

    @GetMapping("/{guildId}/channels/{channelId}/messages")
    public ResponseEntity<List<MessageReadModel>> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
        @PathVariable Long channelId,
            @RequestParam(required = false) Long around,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "50") int limit
    ) {
        Long requesterIdVal = Long.valueOf(jwt.getSubject());

        MessageId aroundId = around != null ? new MessageId(around) : null;
        MessageId beforeId = before != null ? new MessageId(before) : null;
        MessageId afterId  = after != null  ? new MessageId(after) : null;

        GetChannelMessagesUseCase.Command command = new GetChannelMessagesUseCase.Command(
                new GuildId(guildId),
                new ChannelId(channelId),
                new UserId(requesterIdVal),
                aroundId,
                beforeId,
                afterId,
                limit
        );

        List<MessageReadModel> response = getChannelMessagesUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{guildId}/channels/{channelId}/messages/{messageId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guildId,
            @PathVariable Long channelId,
            @PathVariable Long messageId
        ) {

        DeleteMessageUseCase.Command command = new DeleteMessageUseCase.Command(
            new GuildId(guildId),
            new ChannelId(channelId),
            new MessageId(messageId),
            new UserId(Long.parseLong(jwt.getSubject()))
        );
        deleteMessageUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

}
