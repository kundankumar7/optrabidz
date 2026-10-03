package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.event.PayoutTransferFailedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferConfirmedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferRetryStartedEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentPaidEvent;
import com.project.optrabidz.financial.application.event.SettlementConfirmedEvent;
import com.project.optrabidz.financial.application.exception.PayoutTransferStateConflictException;
import com.project.optrabidz.financial.application.payout.PayoutInstruction;
import com.project.optrabidz.financial.application.payout.PayoutProvider;
import com.project.optrabidz.financial.application.payout.PayoutResult;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.AgreementDebtTerms;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutExecutionServiceTest {
    private static final Long TRANSFER_ID = 41L;
    private static final Long PAYMENT_INTENT_ID = 31L;
    private static final Long BINDING_ID = 21L;
    private static final Long SETTLEMENT_ID = 11L;
    private static final Long INSTALLMENT_ID = 12L;
    private static final Long REPAYMENT_ID = 13L;
    private static final Long AGREEMENT_ID = 14L;
    private static final Long PAYER_ACCOUNT_ID = 15L;
    private static final Long PAYEE_ACCOUNT_ID = 16L;
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Mock private PaymentIntentRepository paymentIntentRepository;
    @Mock private PaymentAccountBindingRepository bindingRepository;
    @Mock private PayoutTransferRepository payoutTransferRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private RepaymentInstallmentRepository installmentRepository;
    @Mock private RepaymentRepository repaymentRepository;
    @Mock private AgreementRepository agreementRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private PayoutProvider payoutProvider;

    private PayoutService service;

    @BeforeEach
    void setUp() {
        service = new PayoutService(
                paymentIntentRepository,
                bindingRepository,
                payoutTransferRepository,
                settlementRepository,
                installmentRepository,
                repaymentRepository,
                agreementRepository,
                eventPublisher,
                Optional.of(payoutProvider)
        );
    }

    @Test
    void initialSuccessClaimsPendingTransferAndFinalizesSettlement() {
        PayoutTransfer pending = transfer(PayoutTransferStatus.PENDING, 0);
        PayoutTransfer processing = transfer(PayoutTransferStatus.PROCESSING, 1);
        PayoutTransfer confirmed = transfer(PayoutTransferStatus.CONFIRMED, 1);
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.PENDING), any())).thenReturn(1);
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(processing), Optional.of(confirmed));
        when(bindingRepository.findById(BINDING_ID)).thenReturn(Optional.of(binding()));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(settlementIntent()));
        when(payoutProvider.execute(any())).thenReturn(PayoutResult.confirmed("DEMO-PAYOUT-41"));
        when(payoutTransferRepository.markConfirmed(
                eq(TRANSFER_ID), eq("DEMO-PAYOUT-41"), any())).thenReturn(1);
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(settlement()));
        when(settlementRepository.confirmPayoutPending(
                eq(SETTLEMENT_ID), eq(PAYMENT_INTENT_ID), any())).thenReturn(1);
        when(repaymentRepository.findByAgreementId(AGREEMENT_ID)).thenReturn(Optional.empty());
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement()));
        when(repaymentRepository.save(any())).thenAnswer(invocation -> repaymentWithId(invocation.getArgument(0)));

        PayoutTransfer result = service.executeInitial(pending.getPayoutTransferId());

        assertThat(result.getTransferStatus()).isEqualTo(PayoutTransferStatus.CONFIRMED);
        ArgumentCaptor<PayoutInstruction> instruction = ArgumentCaptor.forClass(PayoutInstruction.class);
        verify(payoutProvider).execute(instruction.capture());
        assertThat(instruction.getValue()).satisfies(value -> {
            assertThat(value.payoutTransferId()).isEqualTo(TRANSFER_ID);
            assertThat(value.externalRecipientReference()).isEqualTo("DEMO-RECIPIENT-21");
            assertThat(value.attemptCount()).isEqualTo(1);
        });
        verify(repaymentRepository).save(any(Repayment.class));
        verify(installmentRepository).saveAll(any());
        verify(eventPublisher).publish(any(PayoutTransferConfirmedEvent.class));
        verify(eventPublisher).publish(any(SettlementConfirmedEvent.class));
    }

    @Test
    void initialFailureKeepsBusinessStatePayoutPending() {
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.PENDING), any())).thenReturn(1);
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(
                        Optional.of(transfer(PayoutTransferStatus.PROCESSING, 1)),
                        Optional.of(transfer(PayoutTransferStatus.FAILED, 1))
                );
        when(bindingRepository.findById(BINDING_ID)).thenReturn(Optional.of(binding()));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(settlementIntent()));
        when(payoutProvider.execute(any())).thenReturn(PayoutResult.failed(
                "DEMO_PAYOUT_FAILED", "Demonstration payout failed on the first attempt"));
        when(payoutTransferRepository.markFailed(
                eq(TRANSFER_ID), eq("DEMO_PAYOUT_FAILED"), any(), any())).thenReturn(1);

        PayoutTransfer result = service.executeInitial(TRANSFER_ID);

        assertThat(result.getTransferStatus()).isEqualTo(PayoutTransferStatus.FAILED);
        verify(settlementRepository, never()).confirmPayoutPending(any(), any(), any());
        verify(repaymentRepository, never()).save(any());
        verify(eventPublisher).publish(any(PayoutTransferFailedEvent.class));
        verify(eventPublisher, never()).publish(any(SettlementConfirmedEvent.class));
    }

    @Test
    void duplicateInitialExecutionReturnsConfirmedTransferWithoutCallingProvider() {
        PayoutTransfer confirmed = transfer(PayoutTransferStatus.CONFIRMED, 1);
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.PENDING), any())).thenReturn(0);
        when(payoutTransferRepository.findById(TRANSFER_ID)).thenReturn(Optional.of(confirmed));

        PayoutTransfer result = service.executeInitial(TRANSFER_ID);

        assertThat(result).isSameAs(confirmed);
        verify(payoutProvider, never()).execute(any());
    }

    @Test
    void duplicateInitialExecutionNeverRetriesFailedTransfer() {
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.PENDING), any())).thenReturn(0);
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(transfer(PayoutTransferStatus.FAILED, 1)));

        assertThatThrownBy(() -> service.executeInitial(TRANSFER_ID))
                .isInstanceOf(PayoutTransferStateConflictException.class);

        verify(payoutProvider, never()).execute(any());
    }

    @Test
    void manualRetryRejectsAlreadyConfirmedTransfer() {
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(transfer(PayoutTransferStatus.CONFIRMED, 1)));
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, PAYER_ACCOUNT_ID))
                .thenReturn(Optional.of(settlementIntent()));
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.FAILED), any())).thenReturn(0);

        assertThatThrownBy(() -> service.retryFailed(
                PAYER_ACCOUNT_ID,
                com.project.optrabidz.identity.domain.model.RoleType.INVESTOR,
                TRANSFER_ID
        )).isInstanceOf(PayoutTransferStateConflictException.class);

        verify(payoutProvider, never()).execute(any());
    }

    @Test
    void retryUsesCapturedBindingAndFinalizesInstallment() {
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(
                        Optional.of(transfer(PayoutTransferStatus.FAILED, 1)),
                        Optional.of(transfer(PayoutTransferStatus.PROCESSING, 2)),
                        Optional.of(transfer(PayoutTransferStatus.CONFIRMED, 2))
                );
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, PAYER_ACCOUNT_ID))
                .thenReturn(Optional.of(repaymentIntent()));
        when(payoutTransferRepository.claimForProcessing(
                eq(TRANSFER_ID), eq(PayoutTransferStatus.FAILED), any())).thenReturn(1);
        when(bindingRepository.findById(BINDING_ID)).thenReturn(Optional.of(binding()));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(repaymentIntent()));
        when(payoutProvider.execute(any())).thenReturn(PayoutResult.confirmed("DEMO-PAYOUT-41"));
        when(payoutTransferRepository.markConfirmed(
                eq(TRANSFER_ID), eq("DEMO-PAYOUT-41"), any())).thenReturn(1);
        when(installmentRepository.findById(INSTALLMENT_ID))
                .thenReturn(Optional.of(installment()));
        when(installmentRepository.confirmPayoutPending(
                eq(INSTALLMENT_ID), eq(PAYMENT_INTENT_ID), any())).thenReturn(1);
        when(repaymentRepository.findById(REPAYMENT_ID)).thenReturn(Optional.of(repayment()));

        PayoutTransfer result = service.retryFailed(
                PAYER_ACCOUNT_ID,
                com.project.optrabidz.identity.domain.model.RoleType.STARTUP,
                TRANSFER_ID
        );

        assertThat(result.getTransferStatus()).isEqualTo(PayoutTransferStatus.CONFIRMED);
        verify(bindingRepository).findById(BINDING_ID);
        verify(bindingRepository, never()).findVerifiedByAccountIdAndProviderCode(any(), any());
        verify(repaymentRepository).refreshStatus(eq(REPAYMENT_ID), any());
        verify(eventPublisher).publish(any(PayoutTransferRetryStartedEvent.class));
        verify(eventPublisher).publish(any(RepaymentInstallmentPaidEvent.class));
    }

    private static PayoutTransfer transfer(PayoutTransferStatus status, int attemptCount) {
        PayoutTransfer.Builder builder = PayoutTransfer.builder()
                .payoutTransferId(TRANSFER_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentAccountBindingId(BINDING_ID)
                .providerCode("DEMO")
                .idempotencyKey("PAYOUT-PAYMENT-INTENT-31")
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .transferStatus(status)
                .attemptCount(attemptCount)
                .createdAt(NOW)
                .updatedAt(NOW.plusSeconds(attemptCount));
        if (attemptCount > 0) {
            builder.lastAttemptAt(NOW.plusSeconds(1));
        }
        if (status == PayoutTransferStatus.CONFIRMED) {
            builder.providerTransferReference("DEMO-PAYOUT-41")
                    .confirmedAt(NOW.plusSeconds(attemptCount));
        }
        if (status == PayoutTransferStatus.FAILED) {
            builder.latestFailureAt(NOW.plusSeconds(attemptCount))
                    .latestFailureCode("DEMO_PAYOUT_FAILED")
                    .latestFailureMessage("Demonstration payout failed on the first attempt");
        }
        return builder.build();
    }

    private static PaymentAccountBinding binding() {
        return PaymentAccountBinding.builder()
                .paymentAccountBindingId(BINDING_ID)
                .accountId(PAYEE_ACCOUNT_ID)
                .providerCode("DEMO")
                .commandIdempotencyKey("binding-key")
                .externalRecipientReference("DEMO-RECIPIENT-21")
                .bindingStatus(PaymentAccountBindingStatus.DEACTIVATED)
                .maskedAccountSuffix("4321")
                .demoBankLabel("Demonstration Bank")
                .createdAt(NOW.minusSeconds(100))
                .verifiedAt(NOW.minusSeconds(90))
                .deactivatedAt(NOW.minusSeconds(10))
                .build();
    }

    private static PaymentIntent settlementIntent() {
        return paymentIntent(PaymentPurpose.SETTLEMENT, SETTLEMENT_ID, null);
    }

    private static PaymentIntent repaymentIntent() {
        return paymentIntent(PaymentPurpose.REPAYMENT, null, INSTALLMENT_ID);
    }

    private static PaymentIntent paymentIntent(PaymentPurpose purpose,
                                               Long settlementId,
                                               Long installmentId) {
        return PaymentIntent.builder()
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentPurpose(purpose)
                .settlementId(settlementId)
                .repaymentInstallmentId(installmentId)
                .payerAccountId(PAYER_ACCOUNT_ID)
                .payeeAccountId(PAYEE_ACCOUNT_ID)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .paymentState(PaymentState.PAYMENT_CONFIRMED)
                .idempotencyKey("collection-key")
                .createdAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(600))
                .confirmedAt(NOW.minusSeconds(10))
                .build();
    }

    private static Settlement settlement() {
        return Settlement.builder()
                .settlementId(SETTLEMENT_ID)
                .agreementId(AGREEMENT_ID)
                .startupId(501L)
                .investorId(601L)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .settlementState(SettlementState.SETTLEMENT_PAYOUT_PENDING)
                .confirmedPaymentIntentId(PAYMENT_INTENT_ID)
                .createdAt(NOW.minusSeconds(100))
                .expiresAt(NOW.plusSeconds(600))
                .build();
    }

    private static RepaymentInstallment installment() {
        return RepaymentInstallment.builder()
                .repaymentInstallmentId(INSTALLMENT_ID)
                .repaymentId(REPAYMENT_ID)
                .installmentNumber(1)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .dueAt(NOW.plusSeconds(600))
                .installmentState(RepaymentInstallmentState.PAYOUT_PENDING)
                .paymentStartedAt(NOW.minusSeconds(30))
                .confirmedPaymentIntentId(PAYMENT_INTENT_ID)
                .createdAt(NOW.minusSeconds(100))
                .updatedAt(NOW.minusSeconds(10))
                .build();
    }

    private static Repayment repayment() {
        return Repayment.builder()
                .repaymentId(REPAYMENT_ID)
                .agreementId(AGREEMENT_ID)
                .startupId(501L)
                .investorId(601L)
                .totalRepayableAmount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .totalInstallments(1)
                .repaymentPlanType(RepaymentPlanType.ONE_TIME)
                .repaymentState(com.project.optrabidz.financial.domain.model.RepaymentState.NOT_STARTED)
                .startedAt(NOW.minusSeconds(100))
                .finalDueAt(NOW.plusSeconds(600))
                .createdAt(NOW.minusSeconds(100))
                .updatedAt(NOW.minusSeconds(10))
                .build();
    }

    private static Agreement agreement() {
        return Agreement.builder()
                .agreementId(AGREEMENT_ID)
                .listingId(101L)
                .bidId(102L)
                .startupId(501L)
                .investorId(601L)
                .fundingModel(FundingModel.DEBT)
                .createdAt(NOW.minusSeconds(100))
                .debtTerms(new AgreementDebtTerms(
                        103L,
                        AGREEMENT_ID,
                        new BigDecimal("1250.00"),
                        new BigDecimal("12.00"),
                        2,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(100)
                ))
                .build();
    }

    private static Repayment repaymentWithId(Repayment repayment) {
        return Repayment.builder()
                .repaymentId(REPAYMENT_ID)
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
                .updatedAt(repayment.getUpdatedAt())
                .build();
    }
}
