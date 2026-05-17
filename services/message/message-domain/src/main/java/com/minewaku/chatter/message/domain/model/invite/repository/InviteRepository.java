package com.minewaku.chatter.message.domain.model.invite.repository;

import java.util.Optional;

import com.minewaku.chatter.message.domain.model.invite.model.Code;
import com.minewaku.chatter.message.domain.model.invite.model.Invite;

public interface InviteRepository {
    void save(Invite invite);
    void deleteByCode(Code code);
    Optional<Invite> findByCode(Code code);
}
