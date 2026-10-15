package com.minewaku.chatter.identityaccess.infrastructure.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * The structure rules from {@code contracts/adapter-map.md}. They are plain functions so the negative test can run
 * the very same rules against deliberately wrong fixtures (SC-006).
 */
public final class StructureRules {

    static final String ROOT = "com.minewaku.chatter.identityaccess";
    static final String INFRA = ROOT + ".infrastructure";

    static final String[] CORE_PACKAGES = {
        ROOT + ".user..", ROOT + ".authentication..", ROOT + ".emailverification..", ROOT + ".shared.."
    };

    /** One row of the port to adapter table in the adapter map contract. */
    public record PortRow(String port, String adapterSimpleName, String adapterPackage) {}

    public static final List<PortRow> PORT_MAP = List.of(
            row("user.internal.port.PasswordHasher", "Argon2PasswordHasher", "user.password"),
            row("user.internal.port.repository.UserAccountRepository", "JdbcUserAccountRepository", "user.write.jdbc"),
            row(
                    "user.internal.port.query.UserAccountQueryRepository",
                    "JdbcUserAccountQueryRepository",
                    "user.read.jdbc"),
            row("shared.port.TimeBasedIdGenerator", "SnowflakeIdGenerator", "shared.id"),
            row("shared.port.UniqueStringIdGenerator", "UuidV7IdGenerator", "shared.id"),
            row("authentication.internal.port.AccessTokenIssuer", "JwtAccessTokenIssuer", "authentication"),
            row(
                    "authentication.internal.port.RefreshTokenGenerator",
                    "SecureRandomRefreshTokenGenerator",
                    "authentication"),
            row(
                    "authentication.internal.port.repository.AuthenticationSessionRepository",
                    "RedisAuthenticationSessionRepository",
                    "session.write.redis"),
            row(
                    "authentication.internal.port.query.AuthenticationSessionViewRepository",
                    "RedisAuthenticationSessionViewRepository",
                    "session.read.redis"),
            row(
                    "emailverification.internal.port.repository.EmailVerificationRepository",
                    "RedisEmailVerificationRepository",
                    "verification.write.redis"),
            row(
                    "emailverification.internal.port.VerificationNotifier",
                    "MailVerificationNotifier",
                    "verification.mail"));

    private StructureRules() {}

    private static PortRow row(String port, String adapterSimpleName, String adapterSubPackage) {
        return new PortRow(ROOT + "." + port, adapterSimpleName, INFRA + ".adapter.outbound." + adapterSubPackage);
    }

    /** Rule 1: core imports no infrastructure, storage, token, mail or HTTP library types. */
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
                        "com.nimbusds..",
                        "org.springframework.security..",
                        "jakarta.mail..",
                        "jakarta.servlet..",
                        "org.springframework.web..")
                .as("core must not depend on infrastructure or storage, token, mail or HTTP libraries");
    }

    /** Rule 1b: adapters are never referenced from core. */
    public static ArchRule coreDoesNotReferenceAdapters() {
        return noClasses()
                .that()
                .resideInAnyPackage(CORE_PACKAGES)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(INFRA + ".adapter..")
                .as("adapters must not be referenced from core");
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
                .as("persistence rows, records and their mappers must not be public");
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
