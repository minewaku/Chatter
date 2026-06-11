package com.minewaku.chatter.message.application.port.outbound.query.model;

import java.time.Instant;
import java.util.List;

public record MessageReadModel(
    Long id,
    Long channelId,
    Long senderId,
    Long replyId,
    String content,
    Instant timestamp,
    List<AttachmentReadModel> attachments
) {
    
    public record AttachmentReadModel(
        String fileHash,
        String filename,
        String contentType,
        Long size
    ) {
    }
}
