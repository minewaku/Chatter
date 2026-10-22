package com.minewaku.chatter.identityaccess.presentation.structure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Secrets never reach logs: request records print nothing about their content, and only the error layer logs. */
@AnalyzeClasses(
        packages = "com.minewaku.chatter.identityaccess.presentation",
        importOptions = ImportOption.DoNotIncludeTests.class)
class SecretHandlingTest {

    private static final String SENTINEL = "SENTINEL-secret-value-9!";

    @ArchTest
    static final ArchRule onlyTheErrorLayerUsesALogger = noClasses()
            .that()
            .resideOutsideOfPackage("..presentation.shared.error..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.slf4j.Logger")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.lang.System");

    @Test
    void textualFormOfEveryRequestAndCredentialRecordOmitsItsContent() throws Exception {
        List<Class<?>> records = new ArrayList<>();
        for (JavaClass javaClass : new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minewaku.chatter.identityaccess.presentation")) {
            Class<?> type = javaClass.reflect();
            boolean carriesSecrets = type.getSimpleName().endsWith("Request")
                    || type.getSimpleName().equals("TokenPairResponse");
            if (type.isRecord() && carriesSecrets) {
                records.add(type);
            }
        }

        // eight request records plus the credential pair at the time of writing; new ones are picked up automatically
        assertThat(records).hasSizeGreaterThanOrEqualTo(9);
        for (Class<?> type : records) {
            Object instance = instantiate(type);
            assertThat(instance.toString()).as(type.getSimpleName()).doesNotContain(SENTINEL);
        }
    }

    private static Object instantiate(Class<?> type) throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            values[i] = types[i] == LocalDate.class ? LocalDate.of(1999, 1, 1) : SENTINEL;
        }
        Constructor<?> constructor = type.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }
}
