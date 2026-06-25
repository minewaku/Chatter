package com.minewaku.chatter.message.application.service.query;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.query.CheckGuildAccessUseCase;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CheckGuildAccessApplicationService implements CheckGuildAccessUseCase {

    private final RecipientRepository recipientRepository;

    @Override
    public Boolean handle(Command command) {
        RecipientId recipientId = new RecipientId(command.guildId(), command.userId());
        return recipientRepository.existsById(recipientId);
    }
}
