package com.minewaku.chatter.profile.infrastructure.service.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class UuidV7UniqueStringGenerator implements UniqueStringIdGenerator {

    //RECHECK: implement di
    private final NoArgGenerator generator = Generators.timeBasedEpochGenerator();

    @Override
    public String generate() {
        return generator.generate().toString();
    }
}
