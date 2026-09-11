package com.project.optrabidz.architecture;

import com.project.optrabidz.audit.api.AdminAuditController;
import com.project.optrabidz.classification.api.InvestorPreferenceController;
import com.project.optrabidz.classification.api.StartupClassificationController;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.financial.api.FinancialController;
import com.project.optrabidz.financial.api.LocalPaymentSimulationController;
import com.project.optrabidz.financial.api.PaymentProviderWebhookController;
import com.project.optrabidz.governance.api.AdminRecoveryController;
import com.project.optrabidz.marketplace.api.AgreementController;
import com.project.optrabidz.marketplace.api.BidController;
import com.project.optrabidz.marketplace.api.ListingController;
import com.project.optrabidz.notification.api.NotificationController;
import com.project.optrabidz.participation.api.InvestorController;
import com.project.optrabidz.participation.api.StartupController;
import com.project.optrabidz.security.api.AuthController;
import com.project.optrabidz.security.api.MeController;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(
        packages = "com.project.optrabidz",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ApiSuccessContractArchitectureTest {
    private static final List<Class<?>> CONTRACT_CONTROLLERS = List.of(
            AuthController.class,
            MeController.class,
            StartupController.class,
            InvestorController.class,
            StartupClassificationController.class,
            InvestorPreferenceController.class,
            ListingController.class,
            BidController.class,
            AgreementController.class,
            FinancialController.class,
            LocalPaymentSimulationController.class,
            NotificationController.class,
            AdminAuditController.class,
            AdminRecoveryController.class
    );

    private static final Set<String> CONTRACT_METHODS = Set.of(
            "AuthController#register", "AuthController#login",
            "AuthController#logout", "AuthController#changePassword",
            "MeController#getCurrentUser",
            "StartupController#createStartup", "StartupController#getMyStartup",
            "StartupController#updateStartup",
            "InvestorController#createInvestor", "InvestorController#getMyInvestor",
            "InvestorController#updateInvestor",
            "StartupClassificationController#addMyClassification",
            "StartupClassificationController#replaceMyClassifications",
            "StartupClassificationController#removeMyClassification",
            "StartupClassificationController#getMyClassifications",
            "InvestorPreferenceController#addMyPreference",
            "InvestorPreferenceController#replaceMyPreferences",
            "InvestorPreferenceController#removeMyPreference",
            "InvestorPreferenceController#getMyPreferences",
            "ListingController#createListing", "ListingController#updateListing",
            "ListingController#publishListing", "ListingController#closeListing",
            "ListingController#getMyListings", "ListingController#browseListings",
            "ListingController#recommendedListings", "ListingController#getListing",
            "BidController#submitBid", "BidController#getBid",
            "BidController#getBidsForListing", "BidController#getMyBids",
            "BidController#getMyBidByListing", "BidController#getAcceptedBid",
            "BidController#withdrawBid", "BidController#rejectBid",
            "BidController#acceptBid",
            "AgreementController#getAgreement",
            "AgreementController#getMyStartupAgreements",
            "AgreementController#getMyInvestorAgreements",
            "FinancialController#getSettlement",
            "FinancialController#getMyInvestorSettlements",
            "FinancialController#getMyStartupSettlements",
            "FinancialController#createSettlementPaymentIntent",
            "FinancialController#getRepayment",
            "FinancialController#getRepaymentInstallments",
            "FinancialController#getRepaymentInstallment",
            "FinancialController#getRepaymentProgress",
            "FinancialController#getMyInvestorRepayments",
            "FinancialController#getMyInvestorRepaymentInstallments",
            "FinancialController#getMyStartupRepayments",
            "FinancialController#getMyStartupRepaymentInstallments",
            "FinancialController#createRepaymentPaymentIntent",
            "FinancialController#createRepaymentInstallmentPaymentIntent",
            "FinancialController#getPaymentIntent",
            "FinancialController#createPaymentAttempt",
            "LocalPaymentSimulationController#confirmLocalPaymentAttempt",
            "LocalPaymentSimulationController#failLocalPaymentAttempt",
            "NotificationController#getMyNotifications",
            "NotificationController#getMyNotificationSummary",
            "NotificationController#markRead", "NotificationController#markAllRead",
            "NotificationController#deleteNotification",
            "NotificationController#createSubscription",
            "NotificationController#revokeSubscription",
            "AdminAuditController#searchAuditRecords",
            "AdminRecoveryController#transferAdminAuthority"
    );

    private static final Set<String> EXPLICIT_HTTP_METHODS = Set.of(
            "AuthController#register", "AuthController#logout",
            "AuthController#changePassword",
            "StartupController#createStartup", "InvestorController#createInvestor",
            "StartupClassificationController#addMyClassification",
            "StartupClassificationController#replaceMyClassifications",
            "StartupClassificationController#removeMyClassification",
            "InvestorPreferenceController#addMyPreference",
            "InvestorPreferenceController#replaceMyPreferences",
            "InvestorPreferenceController#removeMyPreference",
            "ListingController#createListing", "BidController#submitBid",
            "FinancialController#createSettlementPaymentIntent",
            "FinancialController#createRepaymentPaymentIntent",
            "FinancialController#createRepaymentInstallmentPaymentIntent",
            "FinancialController#createPaymentAttempt",
            "NotificationController#markRead",
            "NotificationController#deleteNotification",
            "NotificationController#revokeSubscription"
    );

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_DEPEND_ON_RETIRED_SUCCESS_RESPONSES =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().resideInAPackage(
                            "..common.api.response.."
                    )
                    .as("controllers must expose explicit success contracts");

    @ArchTest
    static final ArchRule RETIRED_SUCCESS_RESPONSE_PACKAGE_IS_ABSENT =
            noClasses()
                    .should().resideInAPackage("..common.api.response..")
                    .as("the shared success envelope must stay retired");

    @Test
    void allSixtySixEndpointsUseTheirRequiredReturnContract() {
        Set<String> discoveredControllers = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.project.optrabidz").stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .map(JavaClass::getName)
                .collect(Collectors.toSet());
        Set<String> expectedControllers = Stream.concat(
                        CONTRACT_CONTROLLERS.stream(),
                        Stream.of(PaymentProviderWebhookController.class))
                .map(Class::getName)
                .collect(Collectors.toSet());
        assertThat(discoveredControllers)
                .as("every REST controller must be covered by the API contract inventory")
                .containsExactlyInAnyOrderElementsOf(expectedControllers);

        Map<String, Method> endpoints = CONTRACT_CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(
                        method, RequestMapping.class))
                .collect(Collectors.toMap(
                        ApiSuccessContractArchitectureTest::key,
                        Function.identity()
                ));

        assertThat(endpoints).hasSize(66);
        assertThat(endpoints.keySet()).containsExactlyInAnyOrderElementsOf(
                CONTRACT_METHODS);
        assertThat(EXPLICIT_HTTP_METHODS).hasSize(20);

        endpoints.forEach((methodKey, method) -> {
            if (EXPLICIT_HTTP_METHODS.contains(methodKey)) {
                assertThat(method.getReturnType())
                        .as("%s controls an explicit status or header", methodKey)
                        .isEqualTo(ResponseEntity.class);
            } else {
                assertThat(method.getReturnType())
                        .as("%s returns its success representation directly", methodKey)
                        .isNotEqualTo(ResponseEntity.class)
                        .isNotEqualTo(Void.TYPE);
                assertThat(method.getReturnType() == PageResponse.class
                        || method.getReturnType().getSimpleName().endsWith("Response"))
                        .as("%s must return a DTO or PageResponse", methodKey)
                        .isTrue();
            }
        });
    }

    private static String key(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
