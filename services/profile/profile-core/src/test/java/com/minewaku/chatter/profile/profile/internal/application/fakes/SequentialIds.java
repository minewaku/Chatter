package com.minewaku.chatter.profile.profile.internal.application.fakes;

import com.minewaku.chatter.profile.shared.port.UniqueStringIdGenerator;

/** Hands out {@link ImageIds#at(int)} 1, 2, 3, ... as text. */
public class SequentialIds implements UniqueStringIdGenerator {

    private int next = 1;

    @Override
    public String nextId() {
        return ImageIds.at(next++).value().toString();
    }
}
