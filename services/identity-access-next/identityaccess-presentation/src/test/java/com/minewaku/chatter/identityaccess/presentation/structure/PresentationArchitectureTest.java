package com.minewaku.chatter.identityaccess.presentation.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/** Presentation depends only on core's public contracts (FR-026) and is organised by capability (FR-027). */
@AnalyzeClasses(
        packages = "com.minewaku.chatter.identityaccess.presentation",
        importOptions = ImportOption.DoNotIncludeTests.class)
class PresentationArchitectureTest {

    @ArchTest
    static final ArchRule presentationDoesNotReachInfrastructureOrCoreInternals = noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.minewaku.chatter.identityaccess.infrastructure..",
                    "com.minewaku.chatter.identityaccess..internal..",
                    "org.springframework.data..",
                    "io.lettuce..",
                    "org.postgresql..",
                    "jakarta.mail..",
                    "org.flywaydb..");

    @ArchTest
    static final ArchRule controllersLiveInCapabilityPackages = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAnyPackage("..presentation.authentication..", "..presentation.session..", "..presentation.user..");
}
