package com.minewaku.chatter.message.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.query.GetChannelMessagesUseCase;
import com.minewaku.chatter.message.application.port.outbound.query.mapper.MessageReadMapper;
import com.minewaku.chatter.message.application.port.outbound.query.model.MessageReadModel;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GetChannelMessagesApplicationService implements GetChannelMessagesUseCase {
    
    private final RecipientRepository recipientRepository;
    private final MessageRepository messageRepository;
    private final MessageReadMapper messageReadMapper;

    @Override
    public List<MessageReadModel> handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.requesterId());

        recipientRepository.findById(recipientId)
            .orElseThrow(() -> new EntityNotFoundException("Requester not found or not in guild"));
        
        List<Message> messages;
        int limit = command.limit() > 0 ? command.limit() : 50; // Set default limit nếu client quên truyền

        if (command.around() != null) {
            messages = messageRepository.findMessagesAroundByChannelId(command.channelId(), command.around(), limit);
        } 
        else if (command.before() != null) {
            messages = messageRepository.findMessagesBeforeByChannelId(command.channelId(), command.before(), limit);
        } 
        else if (command.after() != null) {
            messages = messageRepository.findMessagesAfterByChannelId(command.channelId(), command.after(), limit);
        } 
        else {
            messages = messageRepository.findLatestMessagesByChannelId(command.channelId(), limit);
        }

        return messages.stream()
                .map(messageReadMapper::entityToModel)
                .toList();
    }
}
