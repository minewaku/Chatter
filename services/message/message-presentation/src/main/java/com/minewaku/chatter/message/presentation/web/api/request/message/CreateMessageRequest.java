package com.minewaku.chatter.message.presentation.web.api.request.message;

public record CreateMessageRequest (
    Long replyId,
    String content
) {}
