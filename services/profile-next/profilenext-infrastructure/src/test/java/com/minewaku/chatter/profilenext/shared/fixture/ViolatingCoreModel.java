package com.minewaku.chatter.profilenext.shared.fixture;

import com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.InfrastructureMarker;

/** Fixture for the negative architecture test: a class in a core package that reaches into infrastructure. */
public class ViolatingCoreModel {

    private InfrastructureMarker leak;

    public InfrastructureMarker leak() {
        return leak;
    }
}
