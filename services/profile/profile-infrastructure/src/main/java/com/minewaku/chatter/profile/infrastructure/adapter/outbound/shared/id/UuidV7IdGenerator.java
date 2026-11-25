package com.minewaku.chatter.profile.infrastructure.adapter.outbound.shared.id;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.minewaku.chatter.profile.shared.port.UniqueStringIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class UuidV7IdGenerator implements UniqueStringIdGenerator {

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    @Override
    public String nextId() {
        return generator.generate().toString();
    }
}
