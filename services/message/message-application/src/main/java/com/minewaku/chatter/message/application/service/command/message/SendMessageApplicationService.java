package com.minewaku.chatter.message.application.service.command.message;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.message.SendMessageUseCase;
import com.minewaku.chatter.message.domain.model.channel.repository.ChannelRepository;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SendMessageApplicationService implements SendMessageUseCase {
    
    private final MessageRepository messageRepository;
    private final RecipientRepository recipientRepository;
    private final ChannelRepository channelRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;

    @Override
    public Void handle(Command command) {
        channelRepository.findById(command.channelId())
            .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        RecipientId recipientId = new RecipientId(command.guildId(), command.senderId());
        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        if(command.replyId() != null) {
            messageRepository.findByIdInChannel(command.channelId(), command.replyId())
                .orElseThrow(() -> new EntityNotFoundException("Reply message not found"));
        }

        MessageId messageId = new MessageId(timeBasedIdGenerator.generate());
        Message message = Message.createNew(
            messageId,
            command.channelId(),
            command.senderId(),
            command.replyId(),
            new ArrayList<>(),
            command.content()
        );

        messageRepository.save(message);
        return null;
    }
}
