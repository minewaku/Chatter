package com.minewaku.chatter.profilenext.infrastructure.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Spec 012 FR-017 / SC-007: none of the legacy profile machinery comes back. Kafka names are deliberately not banned:
 * profile-next consumes account-deleted events (spec 012), it just never publishes or relays any of its own.
 */
@AnalyzeClasses(packages = "com.minewaku.chatter.profilenext", importOptions = ImportOption.DoNotIncludeTests.class)
class LegacyMachineryTest {

    @ArchTest
    static final ArchRule noClassesNamedAfterLegacyMachinery = noClasses()
            .should()
            .haveSimpleNameContaining("Outbox")
            .orShould()
            .haveSimpleNameContaining("Cdc")
            .orShould()
            .haveSimpleNameContaining("Debezium")
            .orShould()
            .haveSimpleNameContaining("DomainEvent")
            .orShould()
            .haveSimpleNameContaining("IntegrationEvent")
            .orShould()
            .haveSimpleNameContaining("EventDispatcher")
            .orShould()
            .haveSimpleNameContaining("AggregateRoot")
            .orShould()
            .haveSimpleNameContaining("AssetReference")
            .allowEmptyShould(true);
}
