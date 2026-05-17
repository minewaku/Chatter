package com.minewaku.chatter.message.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.invite.model.Code;
import com.minewaku.chatter.message.domain.model.invite.model.Invite;
import com.minewaku.chatter.message.domain.model.invite.repository.InviteRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.InviteJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class InviteRepositoryImpl implements InviteRepository{

    private final InviteJdbcRepository inviteJdbcRepository;

    @Override
    public void save(Invite invite) {
        inviteJdbcRepository.save(invite);
    }

    @Override
    public void deleteByCode(Code code) {
        inviteJdbcRepository.deleteByCode(code.getValue());
    }

    @Override
    public Optional<Invite> findByCode(Code code) {
        return inviteJdbcRepository.findByCode(code.getValue());
    }


    
}
