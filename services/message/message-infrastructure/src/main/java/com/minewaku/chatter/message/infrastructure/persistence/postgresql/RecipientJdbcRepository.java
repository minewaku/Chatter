package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.domain.model.recipient.model.Recipient;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;

public interface RecipientJdbcRepository extends ListCrudRepository<Recipient, RecipientId> {
    
    boolean existsById(RecipientId recipientId);
}
