package com.project.optrabidz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.project.optrabidz",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ApplicationLayerArchitectureTest {
    @ArchTest
    static final ArchRule APPLICATION_CODE_DOES_NOT_DEPEND_ON_HTTP_API_ADAPTERS =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..common.api..")
                    .as("application code must not depend on HTTP API adapters");
}
