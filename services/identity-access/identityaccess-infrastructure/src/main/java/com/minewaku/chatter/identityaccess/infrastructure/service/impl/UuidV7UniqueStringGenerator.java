package com.minewaku.chatter.identityaccess.infrastructure.service.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.uuid.NoArgGenerator;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UuidV7UniqueStringGenerator implements UniqueStringIdGenerator {
    
    private final NoArgGenerator generator;

    @Override
    public String generate() {
        return generator.generate().toString();
    }
}
