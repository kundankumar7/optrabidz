package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.application.dto.response.PayoutTransferResponse;
import com.project.optrabidz.financial.application.exception.PayoutTransferNotFoundException;
import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutTransferAccessTest {
    private static final Long ACCOUNT_ID = 15L;
    private static final Long TRANSFER_ID = 41L;
    private static final Long PAYMENT_INTENT_ID = 31L;
    private static final Long BINDING_ID = 21L;
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

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
    void participantCanReadTransferWithExactSafeRepresentation() {
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(confirmedTransfer()));
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(paymentIntent()));
        when(bindingRepository.findById(BINDING_ID)).thenReturn(Optional.of(binding()));

        PayoutTransferResponse response = service.getById(
                ACCOUNT_ID, RoleType.STARTUP, TRANSFER_ID);

        assertThat(response).isEqualTo(new PayoutTransferResponse(
                TRANSFER_ID,
                PAYMENT_INTENT_ID,
                "DEMO",
                PayoutTransferStatus.CONFIRMED,
                new BigDecimal("1250.00"),
                "INR",
                "•••• 4321",
                2,
                NOW,
                NOW.plusSeconds(2),
                NOW.plusSeconds(3),
                NOW.plusSeconds(1),
                "DEMO_PAYOUT_FAILED"
        ));
    }

    @Test
    void participantCanDiscoverTransferFromKnownPaymentIntent() {
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(paymentIntent()));
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(confirmedTransfer()));
        when(bindingRepository.findById(BINDING_ID)).thenReturn(Optional.of(binding()));

        PayoutTransferResponse response = service.getByPaymentIntentId(
                ACCOUNT_ID, RoleType.STARTUP, PAYMENT_INTENT_ID);

        assertThat(response.payoutTransferId()).isEqualTo(TRANSFER_ID);
    }

    @Test
    void foreignParticipantAndMissingTransferUseSameNotFoundError() {
        when(payoutTransferRepository.findById(TRANSFER_ID))
                .thenReturn(Optional.of(confirmedTransfer()));
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(
                ACCOUNT_ID, RoleType.INVESTOR, TRANSFER_ID))
                .isInstanceOf(PayoutTransferNotFoundException.class);
    }

    private static PayoutTransfer confirmedTransfer() {
        return PayoutTransfer.builder()
                .payoutTransferId(TRANSFER_ID)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentAccountBindingId(BINDING_ID)
                .providerCode("DEMO")
                .providerTransferReference("DEMO-PAYOUT-41")
                .idempotencyKey("PAYOUT-PAYMENT-INTENT-31")
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .transferStatus(PayoutTransferStatus.CONFIRMED)
                .attemptCount(2)
                .createdAt(NOW)
                .lastAttemptAt(NOW.plusSeconds(2))
                .confirmedAt(NOW.plusSeconds(3))
                .latestFailureAt(NOW.plusSeconds(1))
                .latestFailureCode("DEMO_PAYOUT_FAILED")
                .latestFailureMessage("First attempt failed")
                .updatedAt(NOW.plusSeconds(3))
                .build();
    }

    private static PaymentIntent paymentIntent() {
        return PaymentIntent.builder()
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(11L)
                .payerAccountId(ACCOUNT_ID)
                .payeeAccountId(16L)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .paymentState(PaymentState.PAYMENT_CONFIRMED)
                .idempotencyKey("collection-key")
                .createdAt(NOW)
                .expiresAt(NOW.plusSeconds(600))
                .confirmedAt(NOW.plusSeconds(1))
                .build();
    }

    private static PaymentAccountBinding binding() {
        return PaymentAccountBinding.builder()
                .paymentAccountBindingId(BINDING_ID)
                .accountId(16L)
                .providerCode("DEMO")
                .commandIdempotencyKey("binding-key")
                .externalRecipientReference("DEMO-RECIPIENT-21")
                .bindingStatus(PaymentAccountBindingStatus.DEACTIVATED)
                .maskedAccountSuffix("4321")
                .demoBankLabel("Demonstration Bank")
                .createdAt(NOW)
                .verifiedAt(NOW)
                .deactivatedAt(NOW.plusSeconds(5))
                .build();
    }
}
