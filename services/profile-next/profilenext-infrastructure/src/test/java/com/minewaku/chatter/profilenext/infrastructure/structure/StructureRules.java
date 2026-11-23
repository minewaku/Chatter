package com.minewaku.chatter.profilenext.infrastructure.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * The structure rules of identity-access, applied to profile-next (spec 012 FR-016). They are plain functions so the
 * negative test can run the very same rules against deliberately wrong fixtures. The service is an empty skeleton, so
 * every rule tolerates matching nothing and the port-to-adapter map starts empty.
 */
public final class StructureRules {

    static final String ROOT = "com.minewaku.chatter.profilenext";
    static final String INFRA = ROOT + ".infrastructure";

    static final String[] CORE_PACKAGES = {ROOT + ".profile..", ROOT + ".shared.."};

    /** One row of the port to adapter table. */
    public record PortRow(String port, String adapterSimpleName, String adapterPackage) {}

    /** Each row names an outbound port, its single adapter and the adapter package. */
    public static final List<PortRow> PORT_MAP = List.of(
            new PortRow(
                    "com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository",
                    "JdbcPendingUploadRepository",
                    INFRA + ".adapter.outbound.image.pending.jdbc"),
            new PortRow(
                    "com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage",
                    "R2ImageStorage",
                    INFRA + ".adapter.outbound.image.storage.r2"),
            new PortRow(
                    "com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository",
                    "JdbcProfileRepository",
                    INFRA + ".adapter.outbound.profile.write.jdbc"),
            new PortRow(
                    "com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository",
                    "JdbcProfileQueryRepository",
                    INFRA + ".adapter.outbound.profile.read.jdbc"),
            new PortRow(
                    "com.minewaku.chatter.profilenext.shared.port.UniqueStringIdGenerator",
                    "UuidV7IdGenerator",
                    INFRA + ".adapter.outbound.shared.id"));

    private StructureRules() {}

    /** Rule 1: core imports no infrastructure, persistence, web or messaging framework types. */
    public static ArchRule coreHasNoInfrastructureOrLibraryDependencies() {
        return noClasses()
                .that()
                .resideInAnyPackage(CORE_PACKAGES)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        INFRA + "..",
                        "org.springframework.data..",
                        "org.springframework.jdbc..",
                        "org.springframework.kafka..",
                        "org.apache.kafka..",
                        "org.springframework.security..",
                        "jakarta.servlet..",
                        "org.springframework.web..")
                .as("core must not depend on infrastructure, persistence, web or messaging frameworks")
                .allowEmptyShould(true);
    }

    /** Rule 1b: adapters are never referenced from core. */
    public static ArchRule coreDoesNotReferenceAdapters() {
        return noClasses()
                .that()
                .resideInAnyPackage(CORE_PACKAGES)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(INFRA + ".adapter..")
                .as("adapters must not be referenced from core")
                .allowEmptyShould(true);
    }

    /** Rule 3: persistence shapes and their mappers stay package-private to their adapter. */
    public static ArchRule persistenceShapesArePackagePrivate() {
        return noClasses()
                .that()
                .haveSimpleNameEndingWith("Row")
                .or()
                .haveSimpleNameEndingWith("Rows")
                .or()
                .haveSimpleNameEndingWith("Record")
                .or()
                .haveSimpleNameEndingWith("RowMapper")
                .or()
                .haveSimpleNameEndingWith("RecordMapper")
                .and()
                .resideInAPackage(INFRA + "..")
                .should()
                .bePublic()
                .as("persistence rows, records and their mappers must not be public")
                .allowEmptyShould(true);
    }

    /** Rule 4: no catch-all folders under infrastructure. */
    public static ArchRule noCatchAllFolders() {
        return noClasses()
                .should()
                .resideInAnyPackage(
                        INFRA + "..service..", INFRA + "..helper..", INFRA + "..config..", INFRA + "..persistence..")
                .as("infrastructure has no service, helper, config or persistence folders")
                .allowEmptyShould(true);
    }

    /** Rule 2: each port has exactly one implementation, with the stated name, at the stated package. */
    public static void assertPortMap(JavaClasses classes, List<PortRow> rows) {
        StringBuilder problems = new StringBuilder();
        for (PortRow row : rows) {
            List<JavaClass> implementations = classes.stream()
                    .filter(c -> !c.isInterface())
                    .filter(c -> c.getPackageName().startsWith(INFRA))
                    .filter(c -> c.isAssignableTo(row.port()))
                    .toList();
            if (implementations.size() != 1) {
                problems.append(row.port())
                        .append(": expected exactly one implementation under infrastructure but found ")
                        .append(implementations.stream().map(JavaClass::getName).toList())
                        .append('\n');
                continue;
            }
            JavaClass implementation = implementations.get(0);
            if (!implementation.getSimpleName().equals(row.adapterSimpleName())
                    || !implementation.getPackageName().equals(row.adapterPackage())) {
                problems.append(row.port())
                        .append(": implementation is ")
                        .append(implementation.getName())
                        .append(" but must be ")
                        .append(row.adapterPackage())
                        .append('.')
                        .append(row.adapterSimpleName())
                        .append('\n');
            }
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("Port to adapter map violated:\n" + problems);
        }
    }

    /** Every outbound port declared in core must be in the map, so a new port forces the table to be updated. */
    public static void assertEveryCorePortIsMapped(JavaClasses classes, List<PortRow> rows) {
        List<String> mapped = rows.stream().map(PortRow::port).toList();
        List<String> unmapped = classes.stream()
                .filter(JavaClass::isInterface)
                .filter(c -> c.getName().startsWith(ROOT + "."))
                .filter(c -> !c.getName().startsWith(INFRA))
                .filter(c -> c.getPackageName().matches(".*\\.internal\\.port(\\..*)?|.*\\.shared\\.port"))
                .map(JavaClass::getName)
                .filter(name -> !mapped.contains(name))
                .toList();
        if (!unmapped.isEmpty()) {
            throw new AssertionError("Core ports missing from the adapter map: " + unmapped);
        }
    }
}
