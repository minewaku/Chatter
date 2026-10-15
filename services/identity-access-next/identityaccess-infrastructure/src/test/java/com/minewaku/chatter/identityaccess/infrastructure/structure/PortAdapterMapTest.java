package com.minewaku.chatter.identityaccess.infrastructure.structure;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "com.minewaku.chatter.identityaccess", importOptions = ImportOption.DoNotIncludeTests.class)
class PortAdapterMapTest {

    @ArchTest
    static void everyCoreOutboundPortHasExactlyOneAdapterAtTheDocumentedLocation(JavaClasses classes) {
        StructureRules.assertPortMap(classes, StructureRules.PORT_MAP);
    }

    @ArchTest
    static void everyCoreOutboundPortIsListedInTheAdapterMap(JavaClasses classes) {
        StructureRules.assertEveryCorePortIsMapped(classes, StructureRules.PORT_MAP);
    }
}
