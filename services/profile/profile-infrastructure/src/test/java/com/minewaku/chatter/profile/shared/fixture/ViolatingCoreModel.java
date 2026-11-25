package com.minewaku.chatter.profile.shared.fixture;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.InfrastructureMarker;

/** Fixture for the negative architecture test: a class in a core package that reaches into infrastructure. */
public class ViolatingCoreModel {

    private InfrastructureMarker leak;

    public InfrastructureMarker leak() {
        return leak;
    }
}
