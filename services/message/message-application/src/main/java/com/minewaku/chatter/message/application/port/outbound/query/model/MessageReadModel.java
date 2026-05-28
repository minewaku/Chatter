package com.minewaku.chatter.message.application.port.outbound.query.model;

import java.util.List;

public record MessageReadModel(
    Long channelId,
    Long messageId,
    String content,
    Long senderId,
    Long replyId,
    List<String> assetHashes,
    Long timestamp

) {
    
}
