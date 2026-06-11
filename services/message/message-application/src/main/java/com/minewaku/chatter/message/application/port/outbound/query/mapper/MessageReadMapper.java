package com.minewaku.chatter.message.application.port.outbound.query.mapper;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.message.model.Attachment;
import com.minewaku.chatter.message.domain.model.message.model.Message;

@Component
public class MessageReadMapper {

    public MessageReadModel entityToModel(Message domain) {
        if (domain == null) {
            return null;
        }

        Long replyId = domain.getReplyId() != null ? domain.getReplyId().getValue() : null;
        List<MessageReadModel.AttachmentReadModel> attachmentReadModels = domain.getAttachments() != null
                ? domain.getAttachments().stream()
                    .map(this::mapAttachment)
                    .toList()
                : Collections.emptyList();

        return new MessageReadModel(
                domain.getId().getValue(),
                domain.getChannelId().getValue(),
                domain.getUserId().getValue(),
                replyId,
                domain.getContent(),
                domain.getTimestamp(),
                attachmentReadModels
        );
    }

    private MessageReadModel.AttachmentReadModel mapAttachment(Attachment attachment) {
        if (attachment == null) {
            return null;
        }

        return new MessageReadModel.AttachmentReadModel(
                attachment.getFileHash(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSize()
        );
    }
} 

