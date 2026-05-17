package com.minewaku.chatter.message.domain.model.message.model;

import java.time.Instant;
import java.util.List;

import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Message {
    private MessageId messageId;
    private UserId userId;
    private ChannelId channelId;
    private MessageId replyId;
    private List<String> assetHashes;
    private String content;
    private Instant timestamp;

    private Message(
            @NonNull MessageId messageId, 
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            @NonNull MessageId replyId,
            @NonNull List<String> assetHashes, 
            @NonNull String content, 
            @NonNull Instant timestamp) {

        this.messageId = messageId;
        this.channelId = channelId;
        this.userId = userId;
        this.replyId = replyId;
        this.assetHashes = assetHashes;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static Message createNew(
            @NonNull MessageId messageId,
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            MessageId replyId,
            @NonNull List<String> assetHashes,
            @NonNull String content
    ) {
        return new Message(
            messageId,
            channelId,
            userId,
            null,
            assetHashes,
            content,
            Instant.now()
        );
    }

    public static Message reconstitute (
            @NonNull MessageId messageId, 
            @NonNull ChannelId channelId, 
            @NonNull UserId userId,
            @NonNull MessageId replyId,
            @NonNull List<String> assetHashes, 
            @NonNull String content, 
            @NonNull Instant timestamp
    ) {
        return new Message(
            messageId,
            channelId,
            userId,
            replyId,
            assetHashes,
            content,
            timestamp
        );
    }

    public boolean addAssetHash(String assetHash) {
        if (assetHashes.contains(assetHash)) {
            return false;
        }
        assetHashes.add(assetHash);
        return true;
    }
}
