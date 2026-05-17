package com.minewaku.chatter.message.application.service.command.message;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.message.DeleteMessageUseCase;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeleteMessageApplicationService implements DeleteMessageUseCase {

    private final RecipientRepository recipientRepository;
    private final MessageRepository messageRepository;

    @Override
    public Void handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));
    
        messageRepository.deleteByIdInChannel(command.channelId(), command.messageId());
        return null;
    }
}
