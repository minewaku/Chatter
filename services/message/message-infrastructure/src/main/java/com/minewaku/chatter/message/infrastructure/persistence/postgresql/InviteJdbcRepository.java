package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.domain.model.invite.model.Invite;
import com.minewaku.chatter.message.domain.model.invite.model.InviteId;

public interface InviteJdbcRepository extends ListCrudRepository<Invite, InviteId> {
    
    void deleteByCode(String code);
    Optional<Invite> findByCode(String code);
}
