package com.project.optrabidz.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.project.optrabidz")
class ExceptionArchitectureTest {
    @ArchTest
    static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
            freeze(noClasses()
                    .that().resideInAnyPackage("..domain..", "..application..")
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..common.api..",
                            "org.springframework.http..",
                            "org.springframework.web..",
                            "jakarta.servlet.."
                    )
                    .as("domain and application exceptions must remain transport-neutral"));
}
