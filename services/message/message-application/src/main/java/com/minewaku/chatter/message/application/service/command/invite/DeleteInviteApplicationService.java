package com.minewaku.chatter.message.application.service.command.invite;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.invite.DeleteInviteUseCase;
import com.minewaku.chatter.message.domain.model.invite.model.Invite;
import com.minewaku.chatter.message.domain.model.invite.repository.InviteRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeleteInviteApplicationService implements DeleteInviteUseCase {
    
    private final InviteRepository inviteRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {
        Invite invite = inviteRepository.findByCode(command.inviteCode())
            .orElseThrow(() -> new EntityNotFoundException("Invite not found with code: " + command.inviteCode()));

        //recheck
        RecipientId requesterRecipientId = new RecipientId(invite.getGuildId(), command.requesterId());
        recipientRepository.findById(requesterRecipientId)
            .orElseThrow(() -> new EntityNotFoundException("User is not a member of the guild with id: " + invite.getGuildId()));
        
        inviteRepository.deleteByCode(command.inviteCode());
        return null;
    }
}
