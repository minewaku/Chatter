package com.minewaku.chatter.message.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.recipient.model.Recipient;
import com.minewaku.chatter.message.domain.model.recipient.model.RecipientId;
import com.minewaku.chatter.message.domain.model.recipient.repository.RecipientRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.RecipientJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class RecipientRepositoryImpl implements RecipientRepository {

    private final RecipientJdbcRepository recipientJdbcRepository;

    @Override
    //recheck: implement cache
    public void save(Recipient recipient) {
        recipientJdbcRepository.save(recipient);
    }

    @Override
    public void saveAll(Iterable<Recipient> recipients) {
        recipientJdbcRepository.saveAll(recipients);
    }

    @Override
    public void deleteById(RecipientId recipientId) {
        recipientJdbcRepository.deleteById(recipientId);
    }

    @Override
    public Optional<Recipient> findById(RecipientId recipientId) {
        return recipientJdbcRepository.findById(recipientId);
    }
    
}
