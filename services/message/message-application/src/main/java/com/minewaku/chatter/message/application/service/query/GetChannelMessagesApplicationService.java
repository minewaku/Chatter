package com.minewaku.chatter.message.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.query.GetChannelMessagesUseCase;
import com.minewaku.chatter.message.application.port.outbound.query.MessageReadRepository;
import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GetChannelMessagesApplicationService implements GetChannelMessagesUseCase {
    
    private final RecipientRepository recipientRepository;
    private final MessageReadRepository messageReadRepository;


    public List<MessageReadModel> handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        //recheck
        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found"));
        
        return messageReadRepository.findMessagesByChannelId(
            command.channelId(), 
            command.requesterId(), 
            command.around(), 
            command.before(), 
            command.after(), 
            command.limit()
        );
    }
}
