package com.minewaku.chatter.message.application.port.outbound.query.model;

import java.util.List;

//RECHECK: REPLYID SHOULD BE CHANGED INTO A WHOLE MESSAGE MODEL INSTEAD
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
