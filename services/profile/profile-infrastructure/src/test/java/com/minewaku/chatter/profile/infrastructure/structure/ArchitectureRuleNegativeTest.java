package com.minewaku.chatter.profile.infrastructure.structure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.shared.fixture.ViolatingCoreModel;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** The structure rules must fail on a deliberately introduced violation, not only pass on the empty skeleton. */
class ArchitectureRuleNegativeTest {

    @Test
    void coreImportingAnInfrastructureTypeIsRejected() {
        JavaClasses violating = new ClassFileImporter().importClasses(ViolatingCoreModel.class);

        assertThatThrownBy(() -> StructureRules.coreHasNoInfrastructureOrLibraryDependencies()
                        .check(violating))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("ViolatingCoreModel");
    }
}
