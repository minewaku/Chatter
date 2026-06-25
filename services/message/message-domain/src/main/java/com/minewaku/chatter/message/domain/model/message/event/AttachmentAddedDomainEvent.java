package com.minewaku.chatter.message.domain.model.message.event;

import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AttachmentAddedDomainEvent extends DomainEvent {

    private final Long messageId;
    private final Long channelId;
    private final String namespace = Namespace.ATTACHMENT.name();
    private final String fileHash;
    private final String contentType;
    private final String fileName;
    private final Long fileSize;

    public AttachmentAddedDomainEvent(
            @NonNull Long messageId,
            @NonNull Long channelId,
            @NonNull String fileHash,
            @NonNull String contentType,
            @NonNull String fileName,
            @NonNull Long fileSize) {

        this.messageId = messageId;
        this.channelId = channelId;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
