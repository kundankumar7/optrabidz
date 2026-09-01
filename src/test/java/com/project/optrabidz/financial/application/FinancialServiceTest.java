package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.dto.request.CreatePaymentAttemptRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentProgressResponse;
import com.project.optrabidz.financial.application.dto.response.SettlementResponse;
import com.project.optrabidz.financial.application.exception.PaymentAlreadyConfirmedException;
import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentPaidEvent;
import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategy;
import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategyRegistry;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.RepaymentProgress;
import com.project.optrabidz.financial.domain.model.RepaymentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderMethodRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ALREADY_CONFIRMED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ATTEMPT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_EXPIRED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_NOT_ACTIVE;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_METHOD_UNSUPPORTED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_PROVIDER_MISMATCH;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_STATE_CONFLICT;
import static com.project.optrabidz.financial.application.error.FinancialErrors.FINANCIAL_OPERATION_NOT_ALLOWED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_INSTALLMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_STATE_CONFLICT;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_NOT_PAYABLE;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_STATE_CONFLICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {
    private static final long SETTLEMENT_EXPIRY_MINUTES = 30;
    private static final long PAYMENT_INTENT_EXPIRY_MINUTES = 15;

    private static final Long STARTUP_ACCOUNT_ID = 110L;
    private static final Long INVESTOR_ACCOUNT_ID = 220L;
    private static final Long STARTUP_ID = 11L;
    private static final Long INVESTOR_ID = 22L;
    private static final Long LISTING_ID = 101L;
    private static final Long AGREEMENT_ID = 701L;
    private static final Long SETTLEMENT_ID = 801L;
    private static final Long PAYMENT_INTENT_ID = 901L;
    private static final Long PAYMENT_ATTEMPT_ID = 1001L;
    private static final Long REPAYMENT_ID = 9001L;
    private static final Long REPAYMENT_INSTALLMENT_ID = 10001L;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private RepaymentInstallmentRepository repaymentInstallmentRepository;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private FundingListingRepository fundingListingRepository;

    @Mock
    private JpaPaymentProviderMethodRepository paymentProviderMethodRepository;

    @Mock
    private PaymentMethodStrategyRegistry paymentMethodStrategyRegistry;

    @Mock
    private PaymentMethodStrategy paymentMethodStrategy;

    @Mock
    private EventPublisher eventPublisher;

    private FinancialService service;

    @BeforeEach
    void setUp() {
        service = new FinancialService(
                settlementRepository,
                repaymentRepository,
                repaymentInstallmentRepository,
                paymentIntentRepository,
                paymentAttemptRepository,
                startupRepository,
                investorRepository,
                agreementRepository,
                fundingListingRepository,
                paymentProviderMethodRepository,
                paymentMethodStrategyRegistry,
                eventPublisher,
                SETTLEMENT_EXPIRY_MINUTES,
                PAYMENT_INTENT_EXPIRY_MINUTES
        );
    }

    @Test
    void createSettlementForAgreementCreatesPendingSettlementFromAgreementAndListingCurrency() {
        Agreement agreement = agreement();
        FundingListing listing = listing();
        when(settlementRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(fundingListingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));
        when(settlementRepository.save(any(Settlement.class)))
                .thenAnswer(invocation -> withSettlementId(invocation.getArgument(0), SETTLEMENT_ID));

        SettlementResponse response = service.createSettlementForAgreement(agreement);

        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        assertThat(response.agreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(response.startupId()).isEqualTo(STARTUP_ID);
        assertThat(response.investorId()).isEqualTo(INVESTOR_ID);
        assertThat(response.amount()).isEqualByComparingTo("550000.00");
        assertThat(response.currencyCode()).isEqualTo("INR");
        assertThat(response.settlementState()).isEqualTo(SettlementState.SETTLEMENT_PENDING);
        assertThat(Duration.between(response.createdAt(), response.expiresAt()).toMinutes())
                .isEqualTo(SETTLEMENT_EXPIRY_MINUTES);
    }

    @Test
    void createSettlementForAgreementReusesExistingSettlement() {
        Settlement existingSettlement = settlement();
        when(settlementRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.of(existingSettlement));

        SettlementResponse response = service.createSettlementForAgreement(agreement());

        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        assertThat(response.settlementState()).isEqualTo(SettlementState.SETTLEMENT_PENDING);
        verify(fundingListingRepository, never()).findById(any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void administratorReadsSettlementThroughGlobalLookup() {
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement()));
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));

        SettlementResponse response = service.getSettlement(1L, RoleType.ADMIN, SETTLEMENT_ID);

        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        verify(settlementRepository).findById(SETTLEMENT_ID);
        verify(settlementRepository, never()).findByIdForStartup(any(), any());
        verify(settlementRepository, never()).findByIdForInvestor(any(), any());
    }

    @Test
    void startupReadsSettlementThroughStartupScopedLookup() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID)).thenReturn(Optional.of(startup()));
        when(settlementRepository.findByIdForStartup(SETTLEMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(settlement()));
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));

        SettlementResponse response = service.getSettlement(STARTUP_ACCOUNT_ID, RoleType.STARTUP, SETTLEMENT_ID);

        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        verify(settlementRepository).findByIdForStartup(SETTLEMENT_ID, STARTUP_ID);
        verify(settlementRepository, never()).findById(SETTLEMENT_ID);
    }

    @Test
    void investorReadsSettlementThroughInvestorScopedLookup() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(settlement()));
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));

        SettlementResponse response = service.getSettlement(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, SETTLEMENT_ID);

        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        verify(settlementRepository).findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID);
        verify(settlementRepository, never()).findById(SETTLEMENT_ID);
    }

    @Test
    void missingAndNonOwnedSettlementUseSameNeutralFailure() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID)).thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.getSettlement(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, SETTLEMENT_ID),
                SETTLEMENT_NOT_FOUND
        );

        verify(settlementRepository, never()).findById(SETTLEMENT_ID);
    }

    @Test
    void scopedSettlementLookupDoesNotTranslateUnexpectedRepositoryFailure() {
        DataRetrievalFailureException repositoryFailure = new DataRetrievalFailureException("database unavailable");
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID)).thenThrow(repositoryFailure);

        assertThatThrownBy(() -> service.getSettlement(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                SETTLEMENT_ID
        )).isSameAs(repositoryFailure);
    }

    @Test
    void repaymentRoleDenialsHappenBeforeAnyRepositoryLookup() {
        assertPaymentFailure(
                () -> service.getMyInvestorRepayments(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP, 1, 20),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );
        assertPaymentFailure(
                () -> service.getMyInvestorRepaymentInstallments(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP,
                        null, null, 1, 20),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );
        assertPaymentFailure(
                () -> service.getMyStartupRepayments(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, 1, 20),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );
        assertPaymentFailure(
                () -> service.getMyStartupRepaymentInstallments(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR,
                        null, null, 1, 20),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );
        assertPaymentFailure(
                () -> service.createRepaymentInstallmentPaymentIntent(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        REPAYMENT_INSTALLMENT_ID),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );
        assertPaymentFailure(
                () -> service.createRepaymentPaymentIntent(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, REPAYMENT_ID),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );

        verifyNoInteractions(
                startupRepository,
                investorRepository,
                agreementRepository,
                repaymentRepository,
                repaymentInstallmentRepository,
                paymentIntentRepository
        );
    }

    @Test
    void administratorReadsRepaymentResourcesThroughUnrestrictedLookups() {
        when(repaymentRepository.findById(REPAYMENT_ID))
                .thenReturn(Optional.of(repayment()));
        when(repaymentInstallmentRepository.findById(REPAYMENT_INSTALLMENT_ID))
                .thenReturn(Optional.of(repaymentInstallment()));
        when(agreementRepository.findById(AGREEMENT_ID))
                .thenReturn(Optional.of(agreement()));

        assertThat(service.getRepayment(1L, RoleType.ADMIN, REPAYMENT_ID)
                .repaymentId()).isEqualTo(REPAYMENT_ID);
        assertThat(service.getRepaymentInstallment(
                1L, RoleType.ADMIN, REPAYMENT_INSTALLMENT_ID)
                .repaymentInstallmentId()).isEqualTo(REPAYMENT_INSTALLMENT_ID);

        verify(repaymentRepository).findById(REPAYMENT_ID);
        verify(repaymentInstallmentRepository)
                .findById(REPAYMENT_INSTALLMENT_ID);
        verify(repaymentRepository, never()).findByIdForStartup(any(), any());
        verify(repaymentRepository, never()).findByIdForInvestor(any(), any());
        verify(repaymentInstallmentRepository, never())
                .findByIdForStartup(any(), any());
        verify(repaymentInstallmentRepository, never())
                .findByIdForInvestor(any(), any());
    }

    @Test
    void startupReadsRepaymentResourcesThroughStartupScopedLookups() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(repaymentRepository.findByIdForStartup(REPAYMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(repayment()));
        when(repaymentInstallmentRepository.findByIdForStartup(
                REPAYMENT_INSTALLMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(repaymentInstallment()));
        when(agreementRepository.findById(AGREEMENT_ID))
                .thenReturn(Optional.of(agreement()));

        assertThat(service.getRepayment(
                STARTUP_ACCOUNT_ID, RoleType.STARTUP, REPAYMENT_ID)
                .repaymentId()).isEqualTo(REPAYMENT_ID);
        assertThat(service.getRepaymentInstallment(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                REPAYMENT_INSTALLMENT_ID
        ).repaymentInstallmentId()).isEqualTo(REPAYMENT_INSTALLMENT_ID);

        verify(repaymentRepository).findByIdForStartup(REPAYMENT_ID, STARTUP_ID);
        verify(repaymentInstallmentRepository).findByIdForStartup(
                REPAYMENT_INSTALLMENT_ID, STARTUP_ID);
        verify(repaymentRepository, never()).findById(REPAYMENT_ID);
        verify(repaymentInstallmentRepository, never())
                .findById(REPAYMENT_INSTALLMENT_ID);
    }

    @Test
    void investorReadsRepaymentResourcesThroughInvestorScopedLookups() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(investor()));
        when(repaymentRepository.findByIdForInvestor(REPAYMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(repayment()));
        when(repaymentInstallmentRepository.findByIdForInvestor(
                REPAYMENT_INSTALLMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(repaymentInstallment()));
        when(agreementRepository.findById(AGREEMENT_ID))
                .thenReturn(Optional.of(agreement()));

        assertThat(service.getRepayment(
                INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, REPAYMENT_ID)
                .repaymentId()).isEqualTo(REPAYMENT_ID);
        assertThat(service.getRepaymentInstallment(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                REPAYMENT_INSTALLMENT_ID
        ).repaymentInstallmentId()).isEqualTo(REPAYMENT_INSTALLMENT_ID);

        verify(repaymentRepository).findByIdForInvestor(REPAYMENT_ID, INVESTOR_ID);
        verify(repaymentInstallmentRepository).findByIdForInvestor(
                REPAYMENT_INSTALLMENT_ID, INVESTOR_ID);
        verify(repaymentRepository, never()).findById(REPAYMENT_ID);
        verify(repaymentInstallmentRepository, never())
                .findById(REPAYMENT_INSTALLMENT_ID);
    }

    @Test
    void missingAndNonOwnedRepaymentResourcesUseNeutralScopedFailures() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(repaymentRepository.findByIdForStartup(REPAYMENT_ID, STARTUP_ID))
                .thenReturn(Optional.empty());
        when(repaymentInstallmentRepository.findByIdForStartup(
                REPAYMENT_INSTALLMENT_ID, STARTUP_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.getRepayment(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP, REPAYMENT_ID),
                REPAYMENT_NOT_FOUND
        );
        assertPaymentFailure(
                () -> service.getRepaymentInstallment(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        REPAYMENT_INSTALLMENT_ID),
                REPAYMENT_INSTALLMENT_NOT_FOUND
        );

        verify(repaymentRepository, never()).findById(REPAYMENT_ID);
        verify(repaymentInstallmentRepository, never())
                .findById(REPAYMENT_INSTALLMENT_ID);
    }

    @Test
    void startupPaymentCreationUsesScopedResourcesBeforeStateChecks() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(repaymentInstallmentRepository.findByIdForStartup(
                REPAYMENT_INSTALLMENT_ID, STARTUP_ID))
                .thenReturn(Optional.empty());
        when(repaymentRepository.findByIdForStartup(REPAYMENT_ID, STARTUP_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.createRepaymentInstallmentPaymentIntent(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        REPAYMENT_INSTALLMENT_ID),
                REPAYMENT_INSTALLMENT_NOT_FOUND
        );
        assertPaymentFailure(
                () -> service.createRepaymentPaymentIntent(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP, REPAYMENT_ID),
                REPAYMENT_NOT_FOUND
        );

        verify(paymentIntentRepository, never())
                .findActiveByRepaymentInstallmentId(any());
        verify(repaymentInstallmentRepository, never())
                .findNextPayableByRepaymentId(any());
    }

    @Test
    void creationRaceReturnsCanonicalIntentWhenInstallmentIsInProgress() {
        PaymentIntent canonicalIntent = repaymentPaymentIntent(
                PaymentState.CREATED);
        stubRepaymentInstallmentCreationRace(
                repaymentInstallment(
                        RepaymentInstallmentState.PAYMENT_IN_PROGRESS,
                        null),
                canonicalIntent
        );

        PaymentIntentResponse response =
                service.createRepaymentInstallmentPaymentIntent(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        REPAYMENT_INSTALLMENT_ID
                );

        assertThat(response.paymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @ParameterizedTest
    @EnumSource(value = RepaymentInstallmentState.class, names = {
            "PAID", "PAYMENT_FAILED", "OVERDUE", "CANCELLED"
    })
    void creationRaceRejectsIncompatibleLatestState(
            RepaymentInstallmentState latestState
    ) {
        stubRepaymentInstallmentCreationRace(
                repaymentInstallment(latestState, null),
                repaymentPaymentIntent(PaymentState.CREATED)
        );

        assertPaymentFailure(
                () -> service.createRepaymentInstallmentPaymentIntent(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        REPAYMENT_INSTALLMENT_ID),
                REPAYMENT_STATE_CONFLICT
        );

        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void creationRaceUsesInstallmentNotFoundWhenLatestRowDisappears() {
        stubRepaymentInstallmentCreationRace(
                null,
                repaymentPaymentIntent(PaymentState.CREATED)
        );

        assertPaymentFailure(
                () -> service.createRepaymentInstallmentPaymentIntent(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        REPAYMENT_INSTALLMENT_ID),
                REPAYMENT_INSTALLMENT_NOT_FOUND
        );

        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void startupCanViewRepaymentProgressForOwnAgreement() {
        Instant nextDueAt = now().plusSeconds(86_400);
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID)).thenReturn(Optional.of(startup()));
        when(agreementRepository.findByIdForStartup(AGREEMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(agreement()));
        when(repaymentRepository.getProgressByAgreementId(AGREEMENT_ID)).thenReturn(Optional.of(new RepaymentProgress(
                AGREEMENT_ID,
                9001L,
                STARTUP_ID,
                INVESTOR_ID,
                "INR",
                18,
                3,
                15,
                0,
                0,
                0,
                new BigDecimal("636625.00"),
                new BigDecimal("106104.18"),
                new BigDecimal("530520.82"),
                RepaymentState.IN_PROGRESS,
                10001L,
                4,
                nextDueAt
        )));

        RepaymentProgressResponse response = service.getRepaymentProgress(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                AGREEMENT_ID
        );

        assertThat(response.agreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(response.totalInstallments()).isEqualTo(18);
        assertThat(response.paidInstallments()).isEqualTo(3);
        assertThat(response.unpaidInstallments()).isEqualTo(15);
        assertThat(response.totalAmount()).isEqualByComparingTo("636625.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("106104.18");
        assertThat(response.remainingAmount()).isEqualByComparingTo("530520.82");
        assertThat(response.repaymentState()).isEqualTo(RepaymentState.IN_PROGRESS);
        assertThat(response.nextInstallmentId()).isEqualTo(10001L);
        assertThat(response.nextInstallmentNumber()).isEqualTo(4);
        assertThat(response.nextDueAt()).isEqualTo(nextDueAt);
        assertThat(response.debtTerms().repaymentPlanType()).isEqualTo(RepaymentPlanType.INSTALLMENT_MONTHLY);
        verify(agreementRepository).findByIdForStartup(AGREEMENT_ID, STARTUP_ID);
        verify(agreementRepository, never()).findById(AGREEMENT_ID);
    }

    @Test
    void repaymentProgressIsZeroBeforeScheduleIsCreated() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(agreementRepository.findByIdForInvestor(AGREEMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(agreement()));
        when(repaymentRepository.getProgressByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(settlementRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.of(settlement()));

        RepaymentProgressResponse response = service.getRepaymentProgress(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                AGREEMENT_ID
        );

        assertThat(response.totalInstallments()).isZero();
        assertThat(response.paidInstallments()).isZero();
        assertThat(response.unpaidInstallments()).isZero();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.remainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.currencyCode()).isEqualTo("INR");
        assertThat(response.nextInstallmentId()).isNull();
        verify(agreementRepository).findByIdForInvestor(AGREEMENT_ID, INVESTOR_ID);
        verify(agreementRepository, never()).findById(AGREEMENT_ID);
    }

    @Test
    void missingAndNonOwnedRepaymentProgressUsesNeutralScopedFailure() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(agreementRepository.findByIdForStartup(AGREEMENT_ID, STARTUP_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.getRepaymentProgress(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP, AGREEMENT_ID),
                REPAYMENT_NOT_FOUND
        );

        verify(agreementRepository, never()).findById(AGREEMENT_ID);
        verify(repaymentRepository, never()).getProgressByAgreementId(any());
    }

    @Test
    void investorCanCreateSettlementPaymentIntentForOwnPendingSettlement() {
        Settlement settlement = settlement();
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID)).thenReturn(Optional.of(settlement));
        when(startupRepository.findById(STARTUP_ID)).thenReturn(Optional.of(startup()));
        when(investorRepository.findById(INVESTOR_ID)).thenReturn(Optional.of(investor()));
        when(paymentIntentRepository.findActiveBySettlementId(SETTLEMENT_ID)).thenReturn(Optional.empty());
        when(paymentIntentRepository.saveNewOrFindActiveBySettlement(any(PaymentIntent.class)))
                .thenAnswer(invocation -> withPaymentIntentId(invocation.getArgument(0), PAYMENT_INTENT_ID));

        PaymentIntentResponse response = service.createSettlementPaymentIntent(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                SETTLEMENT_ID
        );

        assertThat(response.paymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
        assertThat(response.paymentPurpose()).isEqualTo(PaymentPurpose.SETTLEMENT);
        assertThat(response.settlementId()).isEqualTo(SETTLEMENT_ID);
        assertThat(response.payerAccountId()).isEqualTo(INVESTOR_ACCOUNT_ID);
        assertThat(response.payeeAccountId()).isEqualTo(STARTUP_ACCOUNT_ID);
        assertThat(response.amount()).isEqualByComparingTo("550000.00");
        assertThat(response.paymentState()).isEqualTo(PaymentState.CREATED);
    }

    @Test
    void investorCannotCreatePaymentIntentForAnotherInvestorSettlement() {
        Investor differentInvestor = new Investor(
                99L,
                999L,
                "Different Investor",
                "Different investor description",
                "Different Investor LLP",
                List.of("https://different.example.com")
        );
        when(investorRepository.findByAccountId(999L)).thenReturn(Optional.of(differentInvestor));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, 99L)).thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.createSettlementPaymentIntent(999L, RoleType.INVESTOR, SETTLEMENT_ID),
                SETTLEMENT_NOT_FOUND
        );

        verify(settlementRepository, never()).findById(SETTLEMENT_ID);
        verify(paymentIntentRepository, never()).findActiveBySettlementId(any());
        verify(paymentIntentRepository, never()).saveNewOrFindActiveBySettlement(any());
    }

    @Test
    void startupCannotCreateSettlementIntentAndNoParticipantOrSettlementLookupOccurs() {
        assertPaymentFailure(
                () -> service.createSettlementPaymentIntent(STARTUP_ACCOUNT_ID, RoleType.STARTUP, SETTLEMENT_ID),
                FINANCIAL_OPERATION_NOT_ALLOWED
        );

        verify(startupRepository, never()).findByAccountId(any());
        verify(investorRepository, never()).findByAccountId(any());
        verify(settlementRepository, never()).findById(any());
        verify(settlementRepository, never()).findByIdForInvestor(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = SettlementState.class, names = {
            "SETTLEMENT_CONFIRMED", "SETTLEMENT_FAILED", "SETTLEMENT_EXPIRED", "SETTLEMENT_CANCELLED"
    })
    void settlementIntentRejectsEveryNonPendingSettlement(SettlementState state) {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(settlement(state, null, now().plusSeconds(3_600))));

        assertPaymentFailure(
                () -> service.createSettlementPaymentIntent(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, SETTLEMENT_ID),
                SETTLEMENT_NOT_PAYABLE
        );

        verify(paymentIntentRepository, never()).findActiveBySettlementId(any());
    }

    @Test
    void settlementIntentRejectsExpiredPendingSettlement() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(settlementRepository.findByIdForInvestor(SETTLEMENT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(settlement(SettlementState.SETTLEMENT_PENDING, null, now().minusSeconds(1))));

        assertPaymentFailure(
                () -> service.createSettlementPaymentIntent(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, SETTLEMENT_ID),
                SETTLEMENT_NOT_PAYABLE
        );
    }

    @Test
    void payerCanCreatePaymentAttemptAndPaymentIntentMovesToPending() {
        PaymentIntent intent = settlementPaymentIntent(PaymentState.CREATED);
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(intent));
        when(paymentProviderMethodRepository.existsByProviderCodeAndMethodTypeAndCurrencyCodeAndEnabledTrue(
                LocalPaymentStrategy.PROVIDER_CODE,
                PaymentMethodType.OTHER,
                "INR"
        )).thenReturn(true);
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(invocation -> withPaymentAttemptIdIfMissing(invocation.getArgument(0), PAYMENT_ATTEMPT_ID));
        when(paymentMethodStrategyRegistry.resolve(LocalPaymentStrategy.PROVIDER_CODE, PaymentMethodType.OTHER))
                .thenReturn(paymentMethodStrategy);
        when(paymentMethodStrategy.initiate(eq(intent), any(PaymentAttempt.class), any(Instant.class)))
                .thenAnswer(invocation -> {
                    PaymentAttempt attempt = invocation.getArgument(1);
                    Instant now = invocation.getArgument(2);
                    attempt.markInitiated("LOCAL-ORDER-1001", "LOCAL-REF-1001", "{\"mode\":\"LOCAL\"}", now);
                    return attempt;
                });
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAttemptResponse response = service.createPaymentAttempt(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                PAYMENT_INTENT_ID,
                new CreatePaymentAttemptRequest(null, null)
        );

        assertThat(response.paymentAttemptId()).isEqualTo(PAYMENT_ATTEMPT_ID);
        assertThat(response.paymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
        assertThat(response.providerCode()).isEqualTo(LocalPaymentStrategy.PROVIDER_CODE);
        assertThat(response.methodType()).isEqualTo(PaymentMethodType.OTHER);
        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.INITIATED);
        assertThat(intent.getPaymentState()).isEqualTo(PaymentState.PAYMENT_PENDING);
    }

    @Test
    void missingParticipantIntentUsesNeutralNotFoundFailure() {
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.getPaymentIntent(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID),
                PAYMENT_INTENT_NOT_FOUND
        );

        verify(paymentIntentRepository).findByIdForParticipant(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID);
        verify(paymentIntentRepository, never()).findById(PAYMENT_INTENT_ID);
    }

    @Test
    void nonOwnedParticipantIntentUsesSameNeutralNotFoundFailure() {
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.getPaymentIntent(STARTUP_ACCOUNT_ID, RoleType.STARTUP, PAYMENT_INTENT_ID),
                PAYMENT_INTENT_NOT_FOUND
        );

        verify(paymentIntentRepository).findByIdForParticipant(PAYMENT_INTENT_ID, STARTUP_ACCOUNT_ID);
        verify(paymentIntentRepository, never()).findById(PAYMENT_INTENT_ID);
    }

    @Test
    void administratorUsesGlobalIntentLookup() {
        PaymentIntent intent = settlementPaymentIntent(PaymentState.CREATED);
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID)).thenReturn(Optional.of(intent));

        PaymentIntentResponse response = service.getPaymentIntent(999L, RoleType.ADMIN, PAYMENT_INTENT_ID);

        assertThat(response.paymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
        verify(paymentIntentRepository).findById(PAYMENT_INTENT_ID);
        verify(paymentIntentRepository, never()).findByIdForParticipant(any(), any());
    }

    @Test
    void nonOwnedPayerIntentUsesNeutralNotFoundFailure() {
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.createPaymentAttempt(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        PAYMENT_INTENT_ID,
                        new CreatePaymentAttemptRequest(null, null)
                ),
                PAYMENT_INTENT_NOT_FOUND
        );

        verify(paymentIntentRepository).findByIdForPayer(PAYMENT_INTENT_ID, STARTUP_ACCOUNT_ID);
        verify(paymentIntentRepository, never()).findById(PAYMENT_INTENT_ID);
    }

    @Test
    void nonOwnedLocalAttemptUsesNeutralNotFoundFailure() {
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        PAYMENT_ATTEMPT_ID
                ),
                PAYMENT_ATTEMPT_NOT_FOUND
        );

        verify(paymentAttemptRepository).findByIdForPayer(PAYMENT_ATTEMPT_ID, STARTUP_ACCOUNT_ID);
        verify(paymentAttemptRepository, never()).findById(PAYMENT_ATTEMPT_ID);
    }

    @Test
    void wrongProviderCallbackUsesNeutralNotFoundFailure() {
        when(paymentAttemptRepository.findByIdForProvider(PAYMENT_ATTEMPT_ID, "UPI"))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.confirmProviderPaymentAttempt("UPI", PAYMENT_ATTEMPT_ID, "UPI-PAYMENT-1001"),
                PAYMENT_ATTEMPT_NOT_FOUND
        );

        verify(paymentAttemptRepository).findByIdForProvider(PAYMENT_ATTEMPT_ID, "UPI");
        verify(paymentAttemptRepository, never()).findById(PAYMENT_ATTEMPT_ID);
        verify(paymentIntentRepository, never()).findById(any());
    }

    @Test
    void ownedNonLocalAttemptOnLocalEndpointUsesProviderMismatchFailure() {
        PaymentAttempt attempt = paymentAttempt("RAZORPAY", PaymentAttemptState.INITIATED);
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt));

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        PAYMENT_ATTEMPT_ID
                ),
                PAYMENT_PROVIDER_MISMATCH
        );
    }

    @Test
    void administratorUsesGlobalAttemptLookupBeforeLocalProviderCheck() {
        PaymentAttempt attempt = paymentAttempt("RAZORPAY", PaymentAttemptState.INITIATED);
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID)).thenReturn(Optional.of(attempt));

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(999L, RoleType.ADMIN, PAYMENT_ATTEMPT_ID),
                PAYMENT_PROVIDER_MISMATCH
        );

        verify(paymentAttemptRepository).findById(PAYMENT_ATTEMPT_ID);
        verify(paymentAttemptRepository, never()).findByIdForPayer(any(), any());
    }

    @Test
    void unsupportedProviderMethodUsesNeutralBusinessRuleFailure() {
        PaymentIntent intent = settlementPaymentIntent(PaymentState.CREATED);
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(intent));

        assertPaymentFailure(
                () -> service.createPaymentAttempt(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        PAYMENT_INTENT_ID,
                        new CreatePaymentAttemptRequest("RAZORPAY", PaymentMethodType.CARD)
                ),
                PAYMENT_METHOD_UNSUPPORTED
        );
    }

    @Test
    void confirmedIntentUsesAlreadyConfirmedFailure() {
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED)));

        assertPaymentFailure(
                () -> service.createPaymentAttempt(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID, null),
                PAYMENT_ALREADY_CONFIRMED
        );
    }

    @Test
    void expiredIntentUsesExpiredFailure() {
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_EXPIRED)));

        assertPaymentFailure(
                () -> service.createPaymentAttempt(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID, null),
                PAYMENT_INTENT_EXPIRED
        );
    }

    @Test
    void otherInactiveIntentUsesNotActiveFailure() {
        when(paymentIntentRepository.findByIdForPayer(PAYMENT_INTENT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_FAILED)));

        assertPaymentFailure(
                () -> service.createPaymentAttempt(
                        INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID, null),
                PAYMENT_INTENT_NOT_ACTIVE
        );
    }

    @Test
    void oppositeAttemptTerminalStateUsesStateConflictFailure() {
        PaymentAttempt failedAttempt = paymentAttempt(LocalPaymentStrategy.PROVIDER_CODE, PaymentAttemptState.FAILED);
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(failedAttempt), Optional.of(failedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_PENDING)));
        when(paymentAttemptRepository.confirmActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID),
                any(Instant.class)
        )).thenReturn(0);

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        PAYMENT_ATTEMPT_ID
                ),
                PAYMENT_STATE_CONFLICT
        );
    }

    @Test
    void confirmingSettlementPaymentAttemptConfirmsSettlementAndCreatesRepaymentSchedule() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement settlement = settlement();
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(repaymentRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));
        when(repaymentRepository.save(any(Repayment.class)))
                .thenAnswer(invocation -> withRepaymentId(invocation.getArgument(0), 9001L));
        when(repaymentInstallmentRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        PaymentAttemptResponse response = service.confirmLocalPaymentAttempt(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                PAYMENT_ATTEMPT_ID
        );

        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        verify(paymentAttemptRepository).confirmActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID),
                any(Instant.class)
        );
        verify(paymentIntentRepository).confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class));
        verify(settlementRepository).confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class));

        ArgumentCaptor<Repayment> repaymentCaptor = ArgumentCaptor.forClass(Repayment.class);
        verify(repaymentRepository).save(repaymentCaptor.capture());
        Repayment repayment = repaymentCaptor.getValue();
        assertThat(repayment.getAgreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(repayment.getStartupId()).isEqualTo(STARTUP_ID);
        assertThat(repayment.getInvestorId()).isEqualTo(INVESTOR_ID);
        assertThat(repayment.getTotalRepayableAmount()).isEqualByComparingTo("636625.00");
        assertThat(repayment.getTotalInstallments()).isEqualTo(18);
        assertThat(repayment.getRepaymentState()).isEqualTo(RepaymentState.NOT_STARTED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RepaymentInstallment>> installmentCaptor = ArgumentCaptor.forClass(List.class);
        verify(repaymentInstallmentRepository).saveAll(installmentCaptor.capture());
        List<RepaymentInstallment> installments = installmentCaptor.getValue();
        RepaymentInstallment firstInstallment = installments.getFirst();
        RepaymentInstallment finalInstallment = installments.getLast();
        assertThat(installments).hasSize(18);
        assertThat(firstInstallment.getRepaymentId()).isEqualTo(9001L);
        assertThat(firstInstallment.getInstallmentNumber()).isEqualTo(1);
        assertThat(firstInstallment.getAmount()).isEqualByComparingTo("35368.06");
        assertThat(firstInstallment.getInstallmentState()).isEqualTo(RepaymentInstallmentState.NOT_STARTED);
        assertThat(finalInstallment.getInstallmentNumber()).isEqualTo(18);
        assertThat(finalInstallment.getAmount()).isEqualByComparingTo("35367.98");
        assertThat(installments.stream().map(RepaymentInstallment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("636625.00");
    }

    @Test
    void confirmingSettlementPaymentAttemptCreatesSingleRepaymentForOneTimePlan() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement settlement = settlement();
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(repaymentRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(oneTimeAgreement()));
        when(repaymentRepository.save(any(Repayment.class)))
                .thenAnswer(invocation -> withRepaymentId(invocation.getArgument(0), 9001L));
        when(repaymentInstallmentRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        service.confirmLocalPaymentAttempt(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                PAYMENT_ATTEMPT_ID
        );

        ArgumentCaptor<Repayment> repaymentCaptor = ArgumentCaptor.forClass(Repayment.class);
        verify(repaymentRepository).save(repaymentCaptor.capture());
        Repayment repayment = repaymentCaptor.getValue();
        assertThat(repayment.getAgreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(repayment.getTotalInstallments()).isEqualTo(1);
        assertThat(repayment.getTotalRepayableAmount()).isEqualByComparingTo("554812.50");
        assertThat(Duration.between(now(), repayment.getFinalDueAt()).toDays()).isBetween(27L, 31L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RepaymentInstallment>> installmentCaptor = ArgumentCaptor.forClass(List.class);
        verify(repaymentInstallmentRepository).saveAll(installmentCaptor.capture());
        assertThat(installmentCaptor.getValue()).singleElement()
                .satisfies(installment -> {
                    assertThat(installment.getInstallmentNumber()).isEqualTo(1);
                    assertThat(installment.getAmount()).isEqualByComparingTo("554812.50");
                });
    }

    @Test
    void repeatedConfirmationBySameIntentIsIdempotent() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement pending = settlement();
        Settlement alreadyConfirmed = settlement(
                SettlementState.SETTLEMENT_CONFIRMED,
                PAYMENT_INTENT_ID,
                now().plusSeconds(3_600)
        );
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(
                eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(pending), Optional.of(alreadyConfirmed));
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class)))
                .thenReturn(0);

        PaymentAttemptResponse response = service.confirmLocalPaymentAttempt(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                PAYMENT_ATTEMPT_ID
        );

        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        verify(repaymentRepository, never()).save(any());
        verify(repaymentInstallmentRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @ParameterizedTest
    @EnumSource(value = SettlementState.class, names = {
            "SETTLEMENT_CONFIRMED", "SETTLEMENT_FAILED", "SETTLEMENT_EXPIRED", "SETTLEMENT_CANCELLED"
    })
    void competingSettlementStateAfterConditionalConfirmationUsesStateConflict(SettlementState state) {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement latest = settlement(
                state,
                state == SettlementState.SETTLEMENT_CONFIRMED ? PAYMENT_INTENT_ID + 1 : null,
                now().plusSeconds(3_600)
        );
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(
                eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(settlement()), Optional.of(latest));
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class)))
                .thenReturn(0);

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        PAYMENT_ATTEMPT_ID
                ),
                SETTLEMENT_STATE_CONFLICT
        );

        verify(repaymentRepository, never()).save(any());
        verify(repaymentInstallmentRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void missingSettlementAfterConditionalConfirmationUsesNeutralNotFoundFailure() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(
                eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(settlement()), Optional.empty());
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class)))
                .thenReturn(0);

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        INVESTOR_ACCOUNT_ID,
                        RoleType.INVESTOR,
                        PAYMENT_ATTEMPT_ID
                ),
                SETTLEMENT_NOT_FOUND
        );

        verify(repaymentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void firstRepaymentConfirmationRefreshesAndPublishesExactlyOnce() {
        stubRepaymentConfirmation(
                1,
                repaymentInstallment(RepaymentInstallmentState.PAID,
                        PAYMENT_INTENT_ID)
        );

        PaymentAttemptResponse response = service.confirmLocalPaymentAttempt(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                PAYMENT_ATTEMPT_ID
        );

        assertThat(response.attemptState())
                .isEqualTo(PaymentAttemptState.CONFIRMED);
        verify(repaymentRepository).refreshStatus(eq(REPAYMENT_ID), any());
        verify(eventPublisher).publish(any(RepaymentInstallmentPaidEvent.class));
    }

    @Test
    void sameIntentRepaymentConfirmationReturnsWithoutDuplicateEffects() {
        stubRepaymentConfirmation(
                0,
                repaymentInstallment(RepaymentInstallmentState.PAID,
                        PAYMENT_INTENT_ID)
        );

        PaymentAttemptResponse response = service.confirmLocalPaymentAttempt(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                PAYMENT_ATTEMPT_ID
        );

        assertThat(response.attemptState())
                .isEqualTo(PaymentAttemptState.CONFIRMED);
        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void competingRepaymentConfirmationUsesStateConflict() {
        stubRepaymentConfirmation(
                0,
                repaymentInstallment(RepaymentInstallmentState.PAID,
                        PAYMENT_INTENT_ID + 1)
        );

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        PAYMENT_ATTEMPT_ID),
                REPAYMENT_STATE_CONFLICT
        );

        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @ParameterizedTest
    @EnumSource(value = RepaymentInstallmentState.class, names = {
            "PAYMENT_FAILED", "OVERDUE", "CANCELLED"
    })
    void incompatibleInstallmentStateAfterConfirmationUsesStateConflict(
            RepaymentInstallmentState latestState
    ) {
        stubRepaymentConfirmation(
                0,
                repaymentInstallment(latestState, null)
        );

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        PAYMENT_ATTEMPT_ID),
                REPAYMENT_STATE_CONFLICT
        );

        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void missingInstallmentAfterConfirmationUsesNeutralNotFoundFailure() {
        stubRepaymentConfirmation(0, null);

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        STARTUP_ACCOUNT_ID,
                        RoleType.STARTUP,
                        PAYMENT_ATTEMPT_ID),
                REPAYMENT_INSTALLMENT_NOT_FOUND
        );

        verify(repaymentRepository, never()).refreshStatus(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void providerCallbackCanConfirmPaymentAttemptThroughSharedCommandPath() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt("LOCAL-PROVIDER-PAYMENT-1001");
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement settlement = settlement();
        when(paymentAttemptRepository.findByIdForProvider(
                PAYMENT_ATTEMPT_ID, LocalPaymentStrategy.PROVIDER_CODE))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(confirmedIntent));
        when(paymentAttemptRepository.confirmActive(eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PROVIDER-PAYMENT-1001"), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(settlementRepository.confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(1);
        when(repaymentRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));
        when(repaymentRepository.save(any(Repayment.class)))
                .thenAnswer(invocation -> withRepaymentId(invocation.getArgument(0), 9001L));
        when(repaymentInstallmentRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        PaymentAttemptResponse response = service.confirmProviderPaymentAttempt(
                LocalPaymentStrategy.PROVIDER_CODE,
                PAYMENT_ATTEMPT_ID,
                "LOCAL-PROVIDER-PAYMENT-1001"
        );

        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        assertThat(response.providerPaymentId()).isEqualTo("LOCAL-PROVIDER-PAYMENT-1001");
        verify(paymentAttemptRepository).confirmActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL-PROVIDER-PAYMENT-1001"),
                any(Instant.class)
        );
        verify(paymentIntentRepository).confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class));
        verify(settlementRepository).confirmPending(eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any(Instant.class));
        verify(repaymentRepository).save(any(Repayment.class));
        verify(repaymentInstallmentRepository).saveAll(any());
    }

    @Test
    void providerCallbackRejectsCompetingConfirmationAfterIntentIsFinalized() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt("LOCAL-PROVIDER-PAYMENT-1001");
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        when(paymentAttemptRepository.findByIdForProvider(
                PAYMENT_ATTEMPT_ID, LocalPaymentStrategy.PROVIDER_CODE))
                .thenReturn(Optional.of(attempt), Optional.of(confirmedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED)));
        when(paymentAttemptRepository.confirmActive(eq(PAYMENT_ATTEMPT_ID), eq("LOCAL-PROVIDER-PAYMENT-1001"), any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(eq(PAYMENT_INTENT_ID), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.confirmProviderPaymentAttempt(
                LocalPaymentStrategy.PROVIDER_CODE,
                PAYMENT_ATTEMPT_ID,
                "LOCAL-PROVIDER-PAYMENT-1001"
        ))
                .isInstanceOf(PaymentAlreadyConfirmedException.class)
                .hasMessageContaining("Payment is already confirmed");

        verify(settlementRepository, never()).confirmPending(any(), any(), any());
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    void providerCallbackRejectsProviderMismatchBeforeConfirmingState() {
        when(paymentAttemptRepository.findByIdForProvider(PAYMENT_ATTEMPT_ID, "UPI"))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.confirmProviderPaymentAttempt(
                        "UPI",
                        PAYMENT_ATTEMPT_ID,
                        "UPI-PAYMENT-1001"
                ),
                PAYMENT_ATTEMPT_NOT_FOUND
        );

        verify(paymentIntentRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).confirmActive(any(), any(), any());
        verify(paymentIntentRepository, never()).confirmActive(any(), any());
        verify(settlementRepository, never()).confirmPending(any(), any(), any());
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    void nonPayerCannotConfirmPaymentAttempt() {
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.confirmLocalPaymentAttempt(
                        STARTUP_ACCOUNT_ID, RoleType.STARTUP, PAYMENT_ATTEMPT_ID),
                PAYMENT_ATTEMPT_NOT_FOUND
        );

        verify(paymentAttemptRepository, never()).save(any());
        verify(paymentIntentRepository, never()).save(any());
        verify(settlementRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).confirmActive(any(), any(), any());
        verify(paymentIntentRepository, never()).confirmActive(any(), any());
        verify(settlementRepository, never()).confirmPending(any(), any(), any());
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    void failingLocalPaymentAttemptFailsAttemptAndIntentAtomically() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt failedAttempt = failedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        when(paymentAttemptRepository.findByIdForPayer(PAYMENT_ATTEMPT_ID, INVESTOR_ACCOUNT_ID))
                .thenReturn(Optional.of(attempt), Optional.of(failedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_FAILED)));
        when(paymentAttemptRepository.failActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL_FAILURE"),
                eq("Local payment failure was simulated"),
                any(Instant.class)
        )).thenReturn(1);
        when(paymentIntentRepository.failActive(
                eq(PAYMENT_INTENT_ID),
                eq("LOCAL_FAILURE"),
                eq("Local payment failure was simulated"),
                any(Instant.class)
        )).thenReturn(1);

        PaymentAttemptResponse response = service.failLocalPaymentAttempt(
                INVESTOR_ACCOUNT_ID,
                RoleType.INVESTOR,
                PAYMENT_ATTEMPT_ID
        );

        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.FAILED);
        assertThat(response.failureCode()).isEqualTo("LOCAL_FAILURE");
        verify(paymentAttemptRepository).failActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL_FAILURE"),
                eq("Local payment failure was simulated"),
                any(Instant.class)
        );
        verify(paymentIntentRepository).failActive(
                eq(PAYMENT_INTENT_ID),
                eq("LOCAL_FAILURE"),
                eq("Local payment failure was simulated"),
                any(Instant.class)
        );
        verify(paymentAttemptRepository, never()).save(any());
        verify(paymentIntentRepository, never()).save(any());
    }

    @Test
    void providerCallbackCanFailPaymentAttemptThroughSharedCommandPath() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt failedAttempt = failedLocalAttempt("UPI_DECLINED", "UPI provider declined the payment");
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        when(paymentAttemptRepository.findByIdForProvider(
                PAYMENT_ATTEMPT_ID, LocalPaymentStrategy.PROVIDER_CODE))
                .thenReturn(Optional.of(attempt), Optional.of(failedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_FAILED)));
        when(paymentAttemptRepository.failActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        )).thenReturn(1);
        when(paymentIntentRepository.failActive(
                eq(PAYMENT_INTENT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        )).thenReturn(1);

        PaymentAttemptResponse response = service.failProviderPaymentAttempt(
                LocalPaymentStrategy.PROVIDER_CODE,
                PAYMENT_ATTEMPT_ID,
                "upi_declined",
                "UPI provider declined the payment"
        );

        assertThat(response.attemptState()).isEqualTo(PaymentAttemptState.FAILED);
        assertThat(response.failureCode()).isEqualTo("UPI_DECLINED");
        verify(paymentAttemptRepository).failActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        );
        verify(paymentIntentRepository).failActive(
                eq(PAYMENT_INTENT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        );
    }

    @Test
    void providerCallbackRejectsCompetingFailureAfterIntentIsFinalized() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt failedAttempt = failedLocalAttempt("UPI_DECLINED", "UPI provider declined the payment");
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        when(paymentAttemptRepository.findByIdForProvider(
                PAYMENT_ATTEMPT_ID, LocalPaymentStrategy.PROVIDER_CODE))
                .thenReturn(Optional.of(attempt), Optional.of(failedAttempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(intent), Optional.of(settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED)));
        when(paymentAttemptRepository.failActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        )).thenReturn(1);
        when(paymentIntentRepository.failActive(
                eq(PAYMENT_INTENT_ID),
                eq("UPI_DECLINED"),
                eq("UPI provider declined the payment"),
                any(Instant.class)
        )).thenReturn(0);

        assertThatThrownBy(() -> service.failProviderPaymentAttempt(
                LocalPaymentStrategy.PROVIDER_CODE,
                PAYMENT_ATTEMPT_ID,
                "upi_declined",
                "UPI provider declined the payment"
        ))
                .isInstanceOf(PaymentAlreadyConfirmedException.class)
                .hasMessageContaining("Payment is already confirmed");
    }

    @Test
    void providerCallbackRejectsFailureProviderMismatchBeforeFailingState() {
        when(paymentAttemptRepository.findByIdForProvider(PAYMENT_ATTEMPT_ID, "CARD"))
                .thenReturn(Optional.empty());

        assertPaymentFailure(
                () -> service.failProviderPaymentAttempt(
                        "CARD",
                        PAYMENT_ATTEMPT_ID,
                        "CARD_DECLINED",
                        "Card provider declined the payment"
                ),
                PAYMENT_ATTEMPT_NOT_FOUND
        );

        verify(paymentIntentRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).failActive(any(), any(), any(), any());
        verify(paymentIntentRepository, never()).failActive(any(), any(), any(), any());
    }

    private void stubRepaymentInstallmentCreationRace(
            RepaymentInstallment latestInstallment,
            PaymentIntent canonicalIntent
    ) {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(repaymentInstallmentRepository.findByIdForStartup(
                REPAYMENT_INSTALLMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(repaymentInstallment()));
        when(repaymentRepository.findByIdForStartup(REPAYMENT_ID, STARTUP_ID))
                .thenReturn(Optional.of(repayment()));
        when(paymentIntentRepository.findActiveByRepaymentInstallmentId(
                REPAYMENT_INSTALLMENT_ID))
                .thenReturn(Optional.empty(), Optional.of(canonicalIntent));
        when(startupRepository.findById(STARTUP_ID))
                .thenReturn(Optional.of(startup()));
        when(investorRepository.findById(INVESTOR_ID))
                .thenReturn(Optional.of(investor()));
        when(paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(
                any(PaymentIntent.class)))
                .thenReturn(canonicalIntent);
        when(repaymentInstallmentRepository.markPaymentInProgress(
                eq(REPAYMENT_INSTALLMENT_ID), any(Instant.class)))
                .thenReturn(0);
        when(repaymentInstallmentRepository.findById(
                        REPAYMENT_INSTALLMENT_ID))
                .thenReturn(Optional.ofNullable(latestInstallment));
    }

    private void stubRepaymentConfirmation(
            int confirmedCount,
            RepaymentInstallment latestInstallment
    ) {
        when(paymentAttemptRepository.findByIdForPayer(
                PAYMENT_ATTEMPT_ID, STARTUP_ACCOUNT_ID))
                .thenReturn(
                        Optional.of(initiatedLocalAttempt()),
                        Optional.of(confirmedLocalAttempt())
                );
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(
                        Optional.of(repaymentPaymentIntent(
                                PaymentState.PAYMENT_PENDING)),
                        Optional.of(repaymentPaymentIntent(
                                PaymentState.PAYMENT_CONFIRMED))
                );
        when(paymentAttemptRepository.confirmActive(
                eq(PAYMENT_ATTEMPT_ID),
                eq("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID),
                any(Instant.class)))
                .thenReturn(1);
        when(paymentIntentRepository.confirmActive(
                eq(PAYMENT_INTENT_ID), any(Instant.class)))
                .thenReturn(1);
        when(repaymentInstallmentRepository.findById(
                REPAYMENT_INSTALLMENT_ID))
                .thenReturn(
                        Optional.of(repaymentInstallment()),
                        Optional.ofNullable(latestInstallment)
                );
        when(repaymentRepository.findById(REPAYMENT_ID))
                .thenReturn(Optional.of(repayment()));
        when(repaymentInstallmentRepository.markPaid(
                eq(REPAYMENT_INSTALLMENT_ID),
                eq(PAYMENT_INTENT_ID),
                any(Instant.class)))
                .thenReturn(confirmedCount);
    }

    private static Agreement agreement() {
        return Agreement.builder()
                .agreementId(AGREEMENT_ID)
                .listingId(LISTING_ID)
                .bidId(501L)
                .startupId(STARTUP_ID)
                .investorId(INVESTOR_ID)
                .fundingModel(FundingModel.DEBT)
                .createdAt(now())
                .debtTerms(new AgreementDebtTerms(
                        601L,
                        AGREEMENT_ID,
                        new BigDecimal("550000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        now()
                ))
                .build();
    }

    private static Agreement oneTimeAgreement() {
        return Agreement.builder()
                .agreementId(AGREEMENT_ID)
                .listingId(LISTING_ID)
                .bidId(501L)
                .startupId(STARTUP_ID)
                .investorId(INVESTOR_ID)
                .fundingModel(FundingModel.DEBT)
                .createdAt(now())
                .debtTerms(new AgreementDebtTerms(
                        601L,
                        AGREEMENT_ID,
                        new BigDecimal("550000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.ONE_TIME,
                        1,
                        now()
                ))
                .build();
    }

    private static FundingListing listing() {
        return FundingListing.builder()
                .listingId(LISTING_ID)
                .startupId(STARTUP_ID)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.AGREEMENT_REACHED)
                .title("Working capital listing")
                .fundingPurposeDescription("Funds needed for inventory expansion.")
                .createdAt(now().minusSeconds(120))
                .publishedAt(now().minusSeconds(60))
                .expiresAt(now().plusSeconds(3_600))
                .closedAt(now())
                .debtTerms(ListingDebtTerms.create(
                        new BigDecimal("550000.00"),
                        "INR",
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        now().minusSeconds(120)
                ))
                .build();
    }

    private static Settlement settlement() {
        return settlement(SettlementState.SETTLEMENT_PENDING, null, now().plusSeconds(3_600));
    }

    private static Settlement settlement(
            SettlementState state,
            Long confirmedPaymentIntentId,
            Instant expiresAt
    ) {
        return Settlement.builder()
                .settlementId(SETTLEMENT_ID)
                .agreementId(AGREEMENT_ID)
                .startupId(STARTUP_ID)
                .investorId(INVESTOR_ID)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .settlementState(state)
                .createdAt(now().minusSeconds(60))
                .expiresAt(expiresAt)
                .confirmedAt(state == SettlementState.SETTLEMENT_CONFIRMED ? now() : null)
                .confirmedPaymentIntentId(confirmedPaymentIntentId)
                .build();
    }

    private static PaymentIntent settlementPaymentIntent(PaymentState paymentState) {
        return PaymentIntent.builder()
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(SETTLEMENT_ID)
                .payerAccountId(INVESTOR_ACCOUNT_ID)
                .payeeAccountId(STARTUP_ACCOUNT_ID)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(paymentState)
                .idempotencyKey("SETTLEMENT-" + SETTLEMENT_ID)
                .createdAt(now().minusSeconds(30))
                .expiresAt(now().plusSeconds(900))
                .build();
    }

    private static PaymentAttempt initiatedLocalAttempt() {
        return PaymentAttempt.builder()
                .paymentAttemptId(PAYMENT_ATTEMPT_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .providerCode(LocalPaymentStrategy.PROVIDER_CODE)
                .methodType(PaymentMethodType.OTHER)
                .providerOrderId("LOCAL-ORDER-1001")
                .providerReferenceId("LOCAL-REF-1001")
                .attemptState(PaymentAttemptState.INITIATED)
                .createdAt(now().minusSeconds(20))
                .initiatedAt(now().minusSeconds(10))
                .providerPayload("{\"mode\":\"LOCAL\"}")
                .build();
    }

    private static PaymentAttempt paymentAttempt(String providerCode, PaymentAttemptState state) {
        return PaymentAttempt.builder()
                .paymentAttemptId(PAYMENT_ATTEMPT_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .providerCode(providerCode)
                .methodType(PaymentMethodType.OTHER)
                .attemptState(state)
                .createdAt(now().minusSeconds(20))
                .build();
    }

    private static void assertPaymentFailure(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable operation,
            ErrorDescriptor descriptor
    ) {
        assertThatThrownBy(operation)
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        failure -> assertThat(failure.descriptor()).isSameAs(descriptor)
                );
    }

    private static PaymentAttempt confirmedLocalAttempt() {
        return confirmedLocalAttempt("LOCAL-PAYMENT-" + PAYMENT_ATTEMPT_ID);
    }

    private static PaymentAttempt confirmedLocalAttempt(String providerPaymentId) {
        return PaymentAttempt.builder()
                .paymentAttemptId(PAYMENT_ATTEMPT_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .providerCode(LocalPaymentStrategy.PROVIDER_CODE)
                .methodType(PaymentMethodType.OTHER)
                .providerOrderId("LOCAL-ORDER-1001")
                .providerPaymentId(providerPaymentId)
                .providerReferenceId("LOCAL-REF-1001")
                .attemptState(PaymentAttemptState.CONFIRMED)
                .createdAt(now().minusSeconds(20))
                .initiatedAt(now().minusSeconds(10))
                .confirmedAt(now())
                .providerPayload("{\"mode\":\"LOCAL\"}")
                .build();
    }

    private static PaymentAttempt failedLocalAttempt() {
        return failedLocalAttempt("LOCAL_FAILURE", "Local payment failure was simulated");
    }

    private static PaymentAttempt failedLocalAttempt(String failureCode, String failureMessage) {
        return PaymentAttempt.builder()
                .paymentAttemptId(PAYMENT_ATTEMPT_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .providerCode(LocalPaymentStrategy.PROVIDER_CODE)
                .methodType(PaymentMethodType.OTHER)
                .providerOrderId("LOCAL-ORDER-1001")
                .providerReferenceId("LOCAL-REF-1001")
                .attemptState(PaymentAttemptState.FAILED)
                .createdAt(now().minusSeconds(20))
                .initiatedAt(now().minusSeconds(10))
                .failedAt(now())
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .providerPayload("{\"mode\":\"LOCAL\"}")
                .build();
    }

    private static Startup startup() {
        return new Startup(
                STARTUP_ID,
                STARTUP_ACCOUNT_ID,
                "Startup One Private Limited",
                "IN",
                "Startup One",
                "Helps startups manage fundraising workflows.",
                List.of("https://startupone.example.com"),
                List.of()
        );
    }

    private static Investor investor() {
        return new Investor(
                INVESTOR_ID,
                INVESTOR_ACCOUNT_ID,
                "Investor One",
                "Early-stage investor focused on SaaS and fintech.",
                "Investor One Ventures LLP",
                List.of("https://investorone.example.com")
        );
    }

    private static Settlement withSettlementId(Settlement settlement, Long settlementId) {
        return Settlement.builder()
                .settlementId(settlementId)
                .agreementId(settlement.getAgreementId())
                .startupId(settlement.getStartupId())
                .investorId(settlement.getInvestorId())
                .amount(settlement.getAmount())
                .currencyCode(settlement.getCurrencyCode())
                .settlementState(settlement.getSettlementState())
                .createdAt(settlement.getCreatedAt())
                .expiresAt(settlement.getExpiresAt())
                .confirmedAt(settlement.getConfirmedAt())
                .failedAt(settlement.getFailedAt())
                .expiredAt(settlement.getExpiredAt())
                .cancelledAt(settlement.getCancelledAt())
                .failureReason(settlement.getFailureReason())
                .confirmedPaymentIntentId(settlement.getConfirmedPaymentIntentId())
                .pspReferenceId(settlement.getPspReferenceId())
                .build();
    }

    private static PaymentIntent withPaymentIntentId(PaymentIntent intent, Long paymentIntentId) {
        return PaymentIntent.builder()
                .paymentIntentId(paymentIntentId)
                .paymentPurpose(intent.getPaymentPurpose())
                .settlementId(intent.getSettlementId())
                .repaymentInstallmentId(intent.getRepaymentInstallmentId())
                .payerAccountId(intent.getPayerAccountId())
                .payeeAccountId(intent.getPayeeAccountId())
                .amount(intent.getAmount())
                .currencyCode(intent.getCurrencyCode())
                .paymentState(intent.getPaymentState())
                .idempotencyKey(intent.getIdempotencyKey())
                .createdAt(intent.getCreatedAt())
                .expiresAt(intent.getExpiresAt())
                .confirmedAt(intent.getConfirmedAt())
                .failedAt(intent.getFailedAt())
                .expiredAt(intent.getExpiredAt())
                .cancelledAt(intent.getCancelledAt())
                .failureCode(intent.getFailureCode())
                .failureMessage(intent.getFailureMessage())
                .build();
    }

    private static PaymentIntent repaymentPaymentIntent(
            PaymentState paymentState
    ) {
        return PaymentIntent.builder()
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentPurpose(PaymentPurpose.REPAYMENT)
                .repaymentInstallmentId(REPAYMENT_INSTALLMENT_ID)
                .payerAccountId(STARTUP_ACCOUNT_ID)
                .payeeAccountId(INVESTOR_ACCOUNT_ID)
                .amount(new BigDecimal("35368.06"))
                .currencyCode("INR")
                .paymentState(paymentState)
                .idempotencyKey(
                        "REPAYMENT-INSTALLMENT-" + REPAYMENT_INSTALLMENT_ID)
                .createdAt(now().minusSeconds(30))
                .expiresAt(now().plusSeconds(900))
                .build();
    }

    private static Repayment repayment() {
        return Repayment.builder()
                .repaymentId(REPAYMENT_ID)
                .agreementId(AGREEMENT_ID)
                .startupId(STARTUP_ID)
                .investorId(INVESTOR_ID)
                .totalRepayableAmount(new BigDecimal("636625.00"))
                .currencyCode("INR")
                .totalInstallments(18)
                .repaymentPlanType(RepaymentPlanType.INSTALLMENT_MONTHLY)
                .repaymentState(RepaymentState.NOT_STARTED)
                .startedAt(now())
                .finalDueAt(now().plus(Duration.ofDays(540)))
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private static RepaymentInstallment repaymentInstallment() {
        return repaymentInstallment(
                RepaymentInstallmentState.NOT_STARTED,
                null
        );
    }

    private static RepaymentInstallment repaymentInstallment(
            RepaymentInstallmentState state,
            Long confirmedPaymentIntentId
    ) {
        return RepaymentInstallment.builder()
                .repaymentInstallmentId(REPAYMENT_INSTALLMENT_ID)
                .repaymentId(REPAYMENT_ID)
                .installmentNumber(1)
                .installmentState(state)
                .amount(new BigDecimal("35368.06"))
                .currencyCode("INR")
                .dueAt(now().plus(Duration.ofDays(30)))
                .paymentStartedAt(
                        state == RepaymentInstallmentState.PAYMENT_IN_PROGRESS
                                ? now() : null)
                .paidAt(state == RepaymentInstallmentState.PAID
                        ? now() : null)
                .failedAt(state == RepaymentInstallmentState.PAYMENT_FAILED
                        ? now() : null)
                .overdueAt(state == RepaymentInstallmentState.OVERDUE
                        ? now() : null)
                .cancelledAt(state == RepaymentInstallmentState.CANCELLED
                        ? now() : null)
                .confirmedPaymentIntentId(confirmedPaymentIntentId)
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private static Repayment withRepaymentId(Repayment repayment, Long repaymentId) {
        return Repayment.builder()
                .repaymentId(repaymentId)
                .agreementId(repayment.getAgreementId())
                .startupId(repayment.getStartupId())
                .investorId(repayment.getInvestorId())
                .totalRepayableAmount(repayment.getTotalRepayableAmount())
                .currencyCode(repayment.getCurrencyCode())
                .totalInstallments(repayment.getTotalInstallments())
                .repaymentPlanType(repayment.getRepaymentPlanType())
                .repaymentState(repayment.getRepaymentState())
                .startedAt(repayment.getStartedAt())
                .finalDueAt(repayment.getFinalDueAt())
                .createdAt(repayment.getCreatedAt())
                .completedAt(repayment.getCompletedAt())
                .cancelledAt(repayment.getCancelledAt())
                .updatedAt(repayment.getUpdatedAt())
                .build();
    }

    private static PaymentAttempt withPaymentAttemptIdIfMissing(PaymentAttempt attempt, Long paymentAttemptId) {
        if (attempt.getPaymentAttemptId() != null) {
            return attempt;
        }
        return PaymentAttempt.builder()
                .paymentAttemptId(paymentAttemptId)
                .paymentIntentId(attempt.getPaymentIntentId())
                .providerCode(attempt.getProviderCode())
                .methodType(attempt.getMethodType())
                .providerOrderId(attempt.getProviderOrderId())
                .providerPaymentId(attempt.getProviderPaymentId())
                .providerReferenceId(attempt.getProviderReferenceId())
                .attemptState(attempt.getAttemptState())
                .createdAt(attempt.getCreatedAt())
                .initiatedAt(attempt.getInitiatedAt())
                .confirmedAt(attempt.getConfirmedAt())
                .failedAt(attempt.getFailedAt())
                .expiredAt(attempt.getExpiredAt())
                .cancelledAt(attempt.getCancelledAt())
                .failureCode(attempt.getFailureCode())
                .failureMessage(attempt.getFailureMessage())
                .providerPayload(attempt.getProviderPayload())
                .build();
    }

    private static Instant now() {
        return Instant.now();
    }
}
