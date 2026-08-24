package com.project.optrabidz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.project.optrabidz",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ExceptionArchitectureTest {
    @ArchTest
    static final ArchRule NEUTRAL_ERROR_CONTRACT_IS_FRAMEWORK_FREE =
            classes()
                    .that().resideInAPackage("..common.error..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "..common.error.."
                    )
                    .as("the neutral error contract may depend only on Java and itself");

    @ArchTest
    static final ArchRule MIGRATED_MODULES_DO_NOT_USE_LEGACY_API_EXCEPTIONS =
            noClasses()
                    .that().resideInAnyPackage(
                            "..identity..",
                            "..security..",
                            "..participation..",
                            "..classification..",
                            "..governance..",
                            "..marketplace..",
                            "..notification.."
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("migrated modules must use the neutral error contract");

    @ArchTest
    static final ArchRule FINANCIAL_USER_CONTROLLERS_DO_NOT_USE_LEGACY_ERRORS =
            noClasses()
                    .that().resideInAPackage("..financial.api..")
                    .and().haveNameMatching(
                            ".*\\.(FinancialController|LocalPaymentSimulationController)"
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("financial user controllers must delegate authentication to Spring Security");

    @ArchTest
    static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
            freeze(noClasses()
                    .that().resideInAnyPackage("..domain..", "..application..")
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..common.api..",
                            "org.springframework.http..",
                            "org.springframework.security..",
                            "org.springframework.web..",
                            "jakarta.servlet.."
                    )
                    .as("domain and application exceptions must remain transport-neutral"));
}
