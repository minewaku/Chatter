package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.exception.EntityNotFoundException;
import com.minewaku.chatter.message.application.port.inbound.command.guild.CreateInviteUseCase;
import com.minewaku.chatter.message.application.port.outbound.service.InviteCodeGenerator;
import com.minewaku.chatter.message.domain.model.guild.repository.GuildRepository;
import com.minewaku.chatter.message.domain.model.invite.model.Code;
import com.minewaku.chatter.message.domain.model.invite.model.Invite;
import com.minewaku.chatter.message.domain.model.invite.model.InviteId;
import com.minewaku.chatter.message.domain.model.invite.repository.InviteRepository;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CreateInviteApplicationService implements CreateInviteUseCase {

    private final GuildRepository guildRepository;
    private final RecipientRepository recipientRepository;
    private final InviteRepository inviteRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final TimeBasedIdGenerator timeBasedIdGenerator;

    @Override
    public Void handle(Command command) {
        guildRepository.findById(command.guildId())
            .orElseThrow(() -> new EntityNotFoundException("Guild not found with id: " + command.guildId()));

        //recheck
        RecipientId inviterRecipientId = new RecipientId(command.guildId(), command.userId());
        recipientRepository.findById(inviterRecipientId)
            .orElseThrow(() -> new EntityNotFoundException("User is not a member of the guild with id: " + command.guildId()));

        Code code = new Code(inviteCodeGenerator.generate());

        InviteId inviteId = new InviteId(timeBasedIdGenerator.generate());
        Invite invite = Invite.createNew(
            inviteId,
            code, 
            command.guildId(), 
            command.userId(), 
            command.maxUses(), 
            command.duration()
        );
        inviteRepository.save(invite);

        return null;   
    }
    
}
