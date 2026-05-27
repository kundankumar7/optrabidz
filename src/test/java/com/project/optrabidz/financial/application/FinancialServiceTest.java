package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.dto.request.CreatePaymentAttemptRequest;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentIntentResponse;
import com.project.optrabidz.financial.application.dto.response.RepaymentProgressResponse;
import com.project.optrabidz.financial.application.dto.response.SettlementResponse;
import com.project.optrabidz.financial.application.exception.FinancialAccessException;
import com.project.optrabidz.financial.application.exception.InvalidPaymentStateException;
import com.project.optrabidz.financial.application.exception.PaymentAlreadyConfirmedException;
import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void startupCanViewRepaymentProgressForOwnAgreement() {
        Instant nextDueAt = now().plusSeconds(86_400);
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID)).thenReturn(Optional.of(startup()));
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
    }

    @Test
    void repaymentProgressIsZeroBeforeScheduleIsCreated() {
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
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
    }

    @Test
    void investorCanCreateSettlementPaymentIntentForOwnPendingSettlement() {
        Settlement settlement = settlement();
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
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
        Settlement settlement = settlement();
        Investor differentInvestor = new Investor(
                99L,
                999L,
                "Different Investor",
                "Different investor description",
                "Different Investor LLP",
                List.of("https://different.example.com")
        );
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(investorRepository.findByAccountId(999L)).thenReturn(Optional.of(differentInvestor));

        assertThatThrownBy(() -> service.createSettlementPaymentIntent(999L, RoleType.INVESTOR, SETTLEMENT_ID))
                .isInstanceOf(FinancialAccessException.class)
                .hasMessageContaining("Investor can pay only own settlement");

        verify(paymentIntentRepository, never()).saveNewOrFindActiveBySettlement(any());
    }

    @Test
    void payerCanCreatePaymentAttemptAndPaymentIntentMovesToPending() {
        PaymentIntent intent = settlementPaymentIntent(PaymentState.CREATED);
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID)).thenReturn(Optional.of(intent));
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
    void confirmingSettlementPaymentAttemptConfirmsSettlementAndCreatesRepaymentSchedule() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement settlement = settlement();
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
    void providerCallbackCanConfirmPaymentAttemptThroughSharedCommandPath() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentAttempt confirmedAttempt = confirmedLocalAttempt("LOCAL-PROVIDER-PAYMENT-1001");
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        PaymentIntent confirmedIntent = settlementPaymentIntent(PaymentState.PAYMENT_CONFIRMED);
        Settlement settlement = settlement();
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        PaymentAttempt attempt = initiatedLocalAttempt();
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.confirmProviderPaymentAttempt(
                "UPI",
                PAYMENT_ATTEMPT_ID,
                "UPI-PAYMENT-1001"
        ))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("Payment attempt does not belong to this provider");

        verify(paymentIntentRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).confirmActive(any(), any(), any());
        verify(paymentIntentRepository, never()).confirmActive(any(), any());
        verify(settlementRepository, never()).confirmPending(any(), any(), any());
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    void nonPayerCannotConfirmPaymentAttempt() {
        PaymentAttempt attempt = initiatedLocalAttempt();
        PaymentIntent intent = settlementPaymentIntent(PaymentState.PAYMENT_PENDING);
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID)).thenReturn(Optional.of(attempt));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID)).thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> service.confirmLocalPaymentAttempt(STARTUP_ACCOUNT_ID, RoleType.STARTUP, PAYMENT_ATTEMPT_ID))
                .isInstanceOf(FinancialAccessException.class)
                .hasMessageContaining("Only payer can perform this payment action");

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
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID))
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
        PaymentAttempt attempt = initiatedLocalAttempt();
        when(paymentAttemptRepository.findById(PAYMENT_ATTEMPT_ID)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.failProviderPaymentAttempt(
                "CARD",
                PAYMENT_ATTEMPT_ID,
                "CARD_DECLINED",
                "Card provider declined the payment"
        ))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("Payment attempt does not belong to this provider");

        verify(paymentIntentRepository, never()).findById(any());
        verify(paymentAttemptRepository, never()).failActive(any(), any(), any(), any());
        verify(paymentIntentRepository, never()).failActive(any(), any(), any(), any());
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
        return Settlement.builder()
                .settlementId(SETTLEMENT_ID)
                .agreementId(AGREEMENT_ID)
                .startupId(STARTUP_ID)
                .investorId(INVESTOR_ID)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .settlementState(SettlementState.SETTLEMENT_PENDING)
                .createdAt(now().minusSeconds(60))
                .expiresAt(now().plusSeconds(3_600))
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
