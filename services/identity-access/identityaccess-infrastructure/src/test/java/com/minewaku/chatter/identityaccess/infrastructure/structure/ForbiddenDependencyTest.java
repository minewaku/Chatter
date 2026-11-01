package com.minewaku.chatter.identityaccess.infrastructure.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Spec 013 FR-010 / FR-011 (amends spec 003 FR-024 and SC-008): writing the account-deleted announcement row is
 * allowed; broker clients, Debezium, CDC code, Vault credential rotation and the legacy auth filters stay banned.
 */
@AnalyzeClasses(
        packages = "com.minewaku.chatter.identityaccess.infrastructure",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ForbiddenDependencyTest {

    @ArchTest
    static final ArchRule noMessagingVaultOrJjwtTypes = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.apache.kafka..",
                    "org.springframework.kafka..",
                    "io.debezium..",
                    "org.springframework.vault..",
                    "org.springframework.cloud.vault..",
                    "io.jsonwebtoken..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noClassesNamedAfterDroppedFeatures = noClasses()
            .should()
            .haveSimpleNameContaining("Debezium")
            .orShould()
            .haveSimpleNameContaining("Cdc")
            .orShould()
            .haveSimpleNameContaining("Kafka")
            .orShould()
            .haveSimpleNameContaining("Vault")
            .orShould()
            .haveSimpleNameContaining("JwtAuthenticationFilter")
            .orShould()
            .haveSimpleNameContaining("GlobalLoggingFilter")
            .orShould()
            .haveSimpleNameContaining("AccessTokenVerifier")
            .allowEmptyShould(true);

    @Test
    void theModulePomDeclaresNoDroppedDependencies() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom)
                .doesNotContain("jjwt")
                .doesNotContain("vault")
                .doesNotContain("kafka")
                .doesNotContain("debezium")
                .doesNotContain("<artifactId>flyway-core</artifactId>");
    }
}
