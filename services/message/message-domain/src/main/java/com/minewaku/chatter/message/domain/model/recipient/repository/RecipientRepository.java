package com.minewaku.chatter.message.domain.model.recipient.repository;

import java.util.Optional;

import com.minewaku.chatter.message.domain.model.recipient.model.Recipient;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;

public interface RecipientRepository {
    void save(Recipient recipient);
    void saveAll(Iterable<Recipient> recipients);
    void deleteById(RecipientId recipientId);
    Optional<Recipient> findById(RecipientId recipientId);
    boolean existsById(RecipientId recipientId);
}
