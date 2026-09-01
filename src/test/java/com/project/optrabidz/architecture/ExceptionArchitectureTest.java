package com.project.optrabidz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
    static final ArchRule PAYMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
            noClasses()
                    .that().haveNameMatching(
                            ".*\\.(PaymentIntentNotFound|PaymentAttemptNotFound|"
                                    + "PaymentIntentExpired|PaymentIntentNotActive|"
                                    + "PaymentAlreadyConfirmed|PaymentStateConflict|"
                                    + "UnsupportedPaymentMethod|PaymentProviderMismatch)Exception"
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("migrated payment exceptions must use the neutral error contract");

    @ArchTest
    static final ArchRule SETTLEMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
            noClasses()
                    .that().haveNameMatching(
                            ".*\\.(SettlementNotFound|SettlementNotPayable|"
                                    + "SettlementStateConflict|"
                                    + "FinancialOperationNotAllowed)Exception"
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("migrated settlement exceptions must use the neutral error contract");

    @ArchTest
    static final ArchRule REPAYMENT_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
            noClasses()
                    .that().haveNameMatching(
                            ".*\\.(RepaymentNotFound|RepaymentInstallmentNotFound|"
                                    + "RepaymentInstallmentNotPayable|"
                                    + "RepaymentStateConflict)Exception"
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("migrated repayment exceptions must use the neutral error contract");

    @ArchTest
    static final ArchRule BUSINESS_EXCEPTIONS_ARE_TRANSPORT_NEUTRAL =
            noClasses()
                    .that().resideInAnyPackage("..domain..", "..application..")
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..common.api..",
                            "org.springframework.http..",
                            "org.springframework.security..",
                            "org.springframework.web..",
                            "jakarta.servlet.."
                    )
                    .as("domain and application exceptions must remain transport-neutral");

    @ArchTest
    static final ArchRule LEGACY_API_EXCEPTION_PACKAGE_IS_ABSENT =
            noClasses()
                    .should().resideInAPackage("..common.api.exception..")
                    .as("the removed legacy API exception package must stay absent");

    @ArchTest
    static final ArchRule PRODUCTION_CODE_DOES_NOT_DEPEND_ON_LEGACY_API_EXCEPTIONS =
            noClasses()
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.exception.."
                    )
                    .as("production code must use the neutral error contract");

    @ArchTest
    static final ArchRule PRODUCTION_CODE_DOES_NOT_DEPEND_INWARD_ON_DOCUMENTATION =
            noClasses()
                    .that().resideOutsideOfPackage("..documentation..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..documentation..")
                    .as("documentation is an outer adapter, never an inward dependency");

    @ArchTest
    static final ArchRule COMPETING_GLOBAL_EXCEPTION_HANDLER_IS_ABSENT =
            noClasses()
                    .should().haveSimpleName("GlobalExceptionHandler")
                    .as("RestExceptionHandler is the single MVC error boundary");

    @ArchTest
    static final ArchRule WEBHOOK_CONTROLLER_HAS_NO_PARSER_OR_PORT_DEPENDENCY =
            noClasses()
                    .that().haveSimpleName("PaymentProviderWebhookController")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.fasterxml.jackson..",
                            "..financial.application.port.."
                    )
                    .as("the webhook controller may only map HTTP and delegate to its ingress boundary");

    @ArchTest
    static final ArchRule WEBHOOK_COMMAND_IS_TRANSPORT_NEUTRAL =
            noClasses()
                    .that().haveSimpleName("PaymentProviderWebhookCommand")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..",
                            "org.springframework.http..",
                            "org.springframework.web.."
                    )
                    .as("the webhook business command must not carry HTTP transport types");

    @ArchTest
    static final ArchRule WEBHOOK_EXCEPTIONS_USE_NEUTRAL_ERROR_CONTRACT =
            noClasses()
                    .that().haveNameMatching(
                            ".*\\.PaymentWebhook(Rejected|PayloadInvalid|ReplayCollision|ReplayState)Exception"
                    )
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.."
                    )
                    .as("webhook exceptions must not depend on API rendering types");

    @ArchTest
    static final ArchRule WEBHOOK_REPLAY_PORT_IS_INFRASTRUCTURE_NEUTRAL =
            noClasses()
                    .that().resideInAPackage("..financial.application.port..")
                    .and().haveSimpleName("PaymentWebhookReplayStore")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..",
                            "org.springframework.http..",
                            "org.springframework.jdbc..",
                            "org.springframework.data..",
                            "com.fasterxml.jackson..",
                            "org.postgresql.."
                    )
                    .as("the webhook replay port must remain independent of transport and persistence frameworks");

    @ArchTest
    static final ArchRule WEBHOOK_CONTROLLER_DOES_NOT_OWN_REPLAY_POLICY =
            noClasses()
                    .that().haveSimpleName("PaymentProviderWebhookController")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..financial.application.replay..",
                            "..financial.infrastructure.repository..",
                            "org.springframework.jdbc..",
                            "org.springframework.data.."
                    )
                    .as("the webhook controller must delegate replay policy to application services");
}
