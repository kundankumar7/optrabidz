package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.exception.PayoutDestinationNotReadyException;
import com.project.optrabidz.financial.application.event.PayoutTransferCreatedEvent;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {
    private static final Long PAYMENT_INTENT_ID = 701L;
    private static final Long PAYEE_ACCOUNT_ID = 801L;
    private static final Long SETTLEMENT_ID = 901L;
    private static final Long BINDING_ID = 1001L;

    @Mock private PaymentIntentRepository paymentIntentRepository;
    @Mock private PaymentAccountBindingRepository bindingRepository;
    @Mock private PayoutTransferRepository payoutTransferRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private RepaymentInstallmentRepository installmentRepository;
    @Mock private RepaymentRepository repaymentRepository;
    @Mock private AgreementRepository agreementRepository;
    @Mock private EventPublisher eventPublisher;

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
                Optional.empty()
        );
    }

    @Test
    void existingTransferIsReturnedWithoutResolvingCurrentBinding() {
        PayoutTransfer existing = transfer(BINDING_ID);
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(existing));

        PayoutTransferEnsureResult result = service.ensureTransfer(PAYMENT_INTENT_ID);

        assertThat(result.transfer()).isSameAs(existing);
        assertThat(result.created()).isFalse();
        verify(bindingRepository, never()).findVerifiedByAccountIdAndProviderCode(any(), any());
        verify(payoutTransferRepository, never()).insertIfAbsent(any());
    }

    @Test
    void createsTransferFromConfirmedCollectionAndCapturesVerifiedBinding() {
        PaymentIntent intent = confirmedSettlementIntent();
        PaymentAccountBinding binding = verifiedBinding();
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty(), Optional.of(transfer(BINDING_ID)));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID)).thenReturn(Optional.of(intent));
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(payoutPendingSettlement()));
        when(bindingRepository.findVerifiedByAccountIdAndProviderCode(PAYEE_ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.of(binding));
        when(payoutTransferRepository.insertIfAbsent(any())).thenReturn(true);

        PayoutTransferEnsureResult result = service.ensureTransfer(PAYMENT_INTENT_ID);

        assertThat(result.created()).isTrue();
        verify(eventPublisher).publish(any(PayoutTransferCreatedEvent.class));
        ArgumentCaptor<PayoutTransfer> transferCaptor = ArgumentCaptor.forClass(PayoutTransfer.class);
        verify(payoutTransferRepository).insertIfAbsent(transferCaptor.capture());
        assertThat(transferCaptor.getValue()).satisfies(transfer -> {
            assertThat(transfer.getPaymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
            assertThat(transfer.getPaymentAccountBindingId()).isEqualTo(BINDING_ID);
            assertThat(transfer.getProviderCode()).isEqualTo("DEMO");
            assertThat(transfer.getIdempotencyKey()).isEqualTo("PAYOUT-PAYMENT-INTENT-701");
            assertThat(transfer.getAmount()).isEqualByComparingTo("1250.00");
            assertThat(transfer.getCurrencyCode()).isEqualTo("INR");
            assertThat(transfer.getTransferStatus()).isEqualTo(PayoutTransferStatus.PENDING);
        });
    }

    @Test
    void concurrentInsertReturnsCommittedWinnerAsNotCreated() {
        PayoutTransfer winner = transfer(BINDING_ID);
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(confirmedSettlementIntent()));
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(payoutPendingSettlement()));
        when(bindingRepository.findVerifiedByAccountIdAndProviderCode(PAYEE_ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.of(verifiedBinding()));
        when(payoutTransferRepository.insertIfAbsent(any())).thenReturn(false);

        PayoutTransferEnsureResult result = service.ensureTransfer(PAYMENT_INTENT_ID);

        assertThat(result.transfer()).isSameAs(winner);
        assertThat(result.created()).isFalse();
        verify(eventPublisher, never()).publish(any(PayoutTransferCreatedEvent.class));
    }

    @Test
    void missingVerifiedBindingIsRetryableAndCreatesNoTransfer() {
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty());
        when(paymentIntentRepository.findById(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(confirmedSettlementIntent()));
        when(settlementRepository.findById(SETTLEMENT_ID))
                .thenReturn(Optional.of(payoutPendingSettlement()));
        when(bindingRepository.findVerifiedByAccountIdAndProviderCode(PAYEE_ACCOUNT_ID, "DEMO"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureTransfer(PAYMENT_INTENT_ID))
                .isInstanceOf(PayoutDestinationNotReadyException.class);

        verify(payoutTransferRepository, never()).insertIfAbsent(any());
    }

    private static PaymentIntent confirmedSettlementIntent() {
        Instant now = Instant.parse("2026-10-02T08:00:00Z");
        return PaymentIntent.builder()
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(SETTLEMENT_ID)
                .payerAccountId(601L)
                .payeeAccountId(PAYEE_ACCOUNT_ID)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .paymentState(PaymentState.PAYMENT_CONFIRMED)
                .idempotencyKey("collection-key")
                .createdAt(now)
                .expiresAt(now.plusSeconds(600))
                .confirmedAt(now.plusSeconds(30))
                .build();
    }

    private static Settlement payoutPendingSettlement() {
        Instant now = Instant.parse("2026-10-02T08:00:00Z");
        return Settlement.builder()
                .settlementId(SETTLEMENT_ID)
                .agreementId(401L)
                .startupId(501L)
                .investorId(601L)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .settlementState(SettlementState.SETTLEMENT_PAYOUT_PENDING)
                .confirmedPaymentIntentId(PAYMENT_INTENT_ID)
                .createdAt(now)
                .expiresAt(now.plusSeconds(600))
                .build();
    }

    private static PaymentAccountBinding verifiedBinding() {
        Instant now = Instant.parse("2026-10-02T08:00:00Z");
        return PaymentAccountBinding.builder()
                .paymentAccountBindingId(BINDING_ID)
                .accountId(PAYEE_ACCOUNT_ID)
                .providerCode("DEMO")
                .commandIdempotencyKey("binding-key")
                .externalRecipientReference("demo-recipient")
                .bindingStatus(com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus.VERIFIED)
                .maskedAccountSuffix("1234")
                .demoBankLabel("Demonstration Bank")
                .createdAt(now)
                .verifiedAt(now.plusSeconds(10))
                .build();
    }

    private static PayoutTransfer transfer(Long bindingId) {
        return PayoutTransfer.builder()
                .payoutTransferId(1101L)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentAccountBindingId(bindingId)
                .providerCode("DEMO")
                .idempotencyKey("PAYOUT-PAYMENT-INTENT-701")
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .transferStatus(PayoutTransferStatus.PENDING)
                .attemptCount(0)
                .createdAt(Instant.parse("2026-10-02T08:01:00Z"))
                .updatedAt(Instant.parse("2026-10-02T08:01:00Z"))
                .build();
    }
}
