package com.minewaku.chatter.identityaccess.infrastructure.structure;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.minewaku.chatter.identityaccess", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreHasNoInfrastructureOrLibraryDependencies =
            StructureRules.coreHasNoInfrastructureOrLibraryDependencies();

    @ArchTest
    static final ArchRule coreDoesNotReferenceAdapters = StructureRules.coreDoesNotReferenceAdapters();

    @ArchTest
    static final ArchRule persistenceShapesArePackagePrivate = StructureRules.persistenceShapesArePackagePrivate();

    @ArchTest
    static final ArchRule noCatchAllFolders = StructureRules.noCatchAllFolders();
}
