package com.minewaku.chatter.profilenext.presentation.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/** Presentation depends only on core's public contracts and is organised by capability, as in identity-access. */
@AnalyzeClasses(
        packages = "com.minewaku.chatter.profilenext.presentation",
        importOptions = ImportOption.DoNotIncludeTests.class)
class PresentationArchitectureTest {

    @ArchTest
    static final ArchRule presentationDoesNotReachInfrastructureOrCoreInternals = noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.minewaku.chatter.profilenext.infrastructure..",
                    "com.minewaku.chatter.profilenext..internal..",
                    "org.springframework.data..",
                    "org.postgresql..",
                    "org.flywaydb..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInCapabilityPackages = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..presentation.*.web..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dtosDoNotDependOnWebLayer = noClasses()
            .that()
            .resideInAPackage("..presentation.*.dto..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation.*.web..")
            .allowEmptyShould(true);
}
