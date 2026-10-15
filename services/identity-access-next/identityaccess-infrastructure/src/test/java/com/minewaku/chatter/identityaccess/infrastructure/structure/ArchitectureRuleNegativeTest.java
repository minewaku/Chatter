package com.minewaku.chatter.identityaccess.infrastructure.structure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.MisplacedAdapter;
import com.minewaku.chatter.identityaccess.user.internal.model.ViolatingCoreModel;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SC-006: the structure rules must fail on a deliberately introduced violation and pass on the delivered code. */
class ArchitectureRuleNegativeTest {

    @Test
    void coreImportingAnInfrastructureTypeIsRejected() {
        JavaClasses violating = new ClassFileImporter().importClasses(ViolatingCoreModel.class);

        assertThatThrownBy(() -> StructureRules.coreHasNoInfrastructureOrLibraryDependencies()
                        .check(violating))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("ViolatingCoreModel");
    }

    @Test
    void anAdapterOutsideItsDocumentedLocationIsRejected() {
        JavaClasses misplaced = new ClassFileImporter().importClasses(MisplacedAdapter.class);
        List<StructureRules.PortRow> passwordHasherOnly = StructureRules.PORT_MAP.stream()
                .filter(row -> row.port().endsWith(".PasswordHasher"))
                .toList();
        assertThat(passwordHasherOnly).hasSize(1);

        assertThatThrownBy(() -> StructureRules.assertPortMap(misplaced, passwordHasherOnly))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("MisplacedAdapter");
    }

    @Test
    void aMissingAdapterIsRejected() {
        JavaClasses none = new ClassFileImporter().importClasses(String.class);

        assertThatThrownBy(() -> StructureRules.assertPortMap(none, StructureRules.PORT_MAP))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected exactly one implementation");
    }
}
