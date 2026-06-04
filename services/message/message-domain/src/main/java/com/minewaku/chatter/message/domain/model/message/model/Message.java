package com.minewaku.chatter.message.domain.model.message.model;

import java.time.Instant;
import java.util.List;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Message {
    private MessageId id;
    private ChannelId channelId;
    private UserId userId;
    //recheck: inside this MessageId the column is named id instead of replyId
    private MessageId replyId;
    private String content;
    private Instant timestamp;

    private List<Attachment> attachments;

    private Message(
            @NonNull MessageId id, 
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            @NonNull MessageId replyId,
            @NonNull String content, 
            @NonNull Instant timestamp,
            @NonNull List<Attachment> attachments) {

        this.id = id;
        this.channelId = channelId;
        this.userId = userId;
        this.replyId = replyId;
        this.content = content;
        this.timestamp = timestamp;
        this.attachments = attachments;
    }

    public static Message createNew(
            @NonNull MessageId id,
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            MessageId replyId,
            @NonNull String content,
            @NonNull List<Attachment> attachments
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
            @NonNull MessageId replyId,
            @NonNull String content, 
            @NonNull Instant timestamp,
            @NonNull List<Attachment> attachments
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

    public boolean addAttachment(String assetHash, String filename, String contentType, Long size) {
        if (assetHash == null || assetHash.trim().isEmpty()) {
            return false;
        }
        boolean isDuplicate = attachments.stream()
                .anyMatch(attachment -> attachment.getFileHash().equals(assetHash));

        if (isDuplicate) {
            return false;
        }

        int newPosition = attachments.size();
        attachments.add(new Attachment(assetHash, filename, contentType, size, newPosition));
        
        return true;
    }

    public boolean removeAttachment(String assetHash) {
        if (assetHash == null || assetHash.trim().isEmpty()) {
            return false;
        }
        boolean isRemoved = attachments.removeIf(attachment -> attachment.getFileHash().equals(assetHash));

        if (!isRemoved) {
            return false;
        }

        for (int i = 0; i < attachments.size(); i++) {
            Attachment current = attachments.get(i);
            if (current.getPosition() != i) {
                attachments.set(i, new Attachment(current.getFileHash(), current.getFilename(), current.getContentType(), current.getSize(), i));
            }
        }

        return true;
    }
}
