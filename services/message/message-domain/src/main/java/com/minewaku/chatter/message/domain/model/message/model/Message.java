package com.minewaku.chatter.message.domain.model.message.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.event.AttachmentAddedDomainEvent;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Message {
    private MessageId id;
    private ChannelId channelId;
    private UserId userId;
    private MessageId replyId;
    private String content;
    private Instant timestamp;

    private List<Attachment> attachments;
    private List<DomainEvent> domainEvents = new ArrayList<>();

    private Message(
            @NonNull MessageId id,
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            MessageId replyId,
            @NonNull String content, 
            @NonNull Instant timestamp,
            List<Attachment> attachments) {

        this.id = id;
        this.channelId = channelId;
        this.userId = userId;
        this.replyId = replyId;
        this.content = content;
        this.timestamp = timestamp;
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
    }

    public static Message createNew(
            @NonNull MessageId id,
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            MessageId replyId,
            @NonNull String content,
            List<Attachment> attachments
    ) {
        return new Message(
            id,
            channelId,
            userId,
            replyId,
            content,
            Instant.now(),
            attachments
        );
    }

    public static Message reconstitute (
            @NonNull MessageId id, 
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            MessageId replyId,
            @NonNull String content, 
            @NonNull Instant timestamp,
            List<Attachment> attachments
    ) {
        return new Message(
            id,
            channelId,
            userId,
            replyId,
            content,
            timestamp,
            attachments
        );
    }

    public boolean addAttachment(
            String assetHash,
            String filename,
            String contentType,
            Long size
    ) {
        if (assetHash == null || assetHash.trim().isEmpty()) {
            return false;
        }

        boolean isDuplicate = attachments.stream()
                .anyMatch(attachment -> attachment.getFileHash().equals(assetHash));

        if (isDuplicate) {
            return false;
        }

        attachments.add(new Attachment(assetHash, filename, contentType, size));
        domainEvents.add(new AttachmentAddedDomainEvent(
                this.getId().getValue(),
                this.getChannelId().getValue(),
                assetHash, contentType, filename, size));

        return true;
    }

    public boolean removeAttachment(String assetHash) {
        if (assetHash == null || assetHash.trim().isEmpty()) {
            return false;
        }
        
        return attachments.removeIf(attachment -> attachment.getFileHash().equals(assetHash));
    }
}
