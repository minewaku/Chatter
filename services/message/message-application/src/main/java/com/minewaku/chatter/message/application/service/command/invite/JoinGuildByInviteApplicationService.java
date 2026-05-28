package com.minewaku.chatter.message.application.service.command.invite;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.invite.JoinGuildByInviteUseCase;
import com.minewaku.chatter.message.domain.model.invite.model.Invite;
import com.minewaku.chatter.message.domain.model.invite.repository.InviteRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.Recipient;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class JoinGuildByInviteApplicationService implements JoinGuildByInviteUseCase {
    
    private final InviteRepository inviteRepository;
    private final RecipientRepository recipientRepository;

    @Override
    public Void handle(Command command) {
        //recheck for expired invite
        Invite invite = inviteRepository.findByCode(command.inviteCode())
            .orElseThrow(() -> new EntityNotFoundException("Invite not found with code: " + command.inviteCode()));

        if(!invite.validateExpiration()) {
            RecipientId newMemberRecipientId = new RecipientId(invite.getGuildId(), command.userId());
            Recipient newMemberRecipient = Recipient.createNew(newMemberRecipientId);
            recipientRepository.save(newMemberRecipient);
        }

        return null;
    }
}
