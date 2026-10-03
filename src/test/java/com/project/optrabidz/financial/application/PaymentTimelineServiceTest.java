package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.dto.response.PaymentTimelineResponse;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineStage;
import com.project.optrabidz.financial.application.dto.response.PaymentTimelineStatus;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTimelineServiceTest {
    private static final Long ACCOUNT_ID = 15L;
    private static final Long PAYMENT_INTENT_ID = 31L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-02T10:00:00Z");
    private static final Instant ATTEMPT_AT = CREATED_AT.plusSeconds(1);
    private static final Instant COLLECTION_AT = CREATED_AT.plusSeconds(2);

    @Mock private PaymentIntentRepository paymentIntentRepository;
    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private PayoutTransferRepository payoutTransferRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private RepaymentInstallmentRepository installmentRepository;

    private PaymentTimelineService service;

    @BeforeEach
    void setUp() {
        service = new PaymentTimelineService(
                paymentIntentRepository,
                paymentAttemptRepository,
                payoutTransferRepository,
                settlementRepository,
                installmentRepository
        );
        when(paymentIntentRepository.findByIdForParticipant(PAYMENT_INTENT_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(confirmedIntent()));
        when(paymentAttemptRepository.findLatestByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(confirmedAttempt()));
    }

    @Test
    void confirmedCollectionWithoutTransferShowsWaitingDestinationFromPersistedTime() {
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.empty());
        when(settlementRepository.findById(11L)).thenReturn(Optional.of(
                settlement(SettlementState.SETTLEMENT_PAYOUT_PENDING, null)));

        PaymentTimelineResponse response = service.getTimeline(
                ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID);

        assertThat(response.demonstration()).isTrue();
        assertThat(response.entries()).extracting(entry -> entry.stage())
                .containsExactly(
                        PaymentTimelineStage.PAYMENT_INTENT_CREATED,
                        PaymentTimelineStage.PAYMENT_ATTEMPT_CREATED,
                        PaymentTimelineStage.DEMO_CHECKOUT_AVAILABLE,
                        PaymentTimelineStage.COLLECTION_CONFIRMED,
                        PaymentTimelineStage.SIMULATED_ESCROW_CREDIT,
                        PaymentTimelineStage.PAYOUT_WAITING_FOR_DESTINATION
                );
        assertThat(response.entries().getLast().status())
                .isEqualTo(PaymentTimelineStatus.PENDING);
        assertThat(response.entries().getLast().occurredAt()).isEqualTo(COLLECTION_AT);
    }

    @Test
    void failedTransferShowsAttemptCountAndFailureWithoutFinalSuccess() {
        when(payoutTransferRepository.findByPaymentIntentId(PAYMENT_INTENT_ID))
                .thenReturn(Optional.of(failedTransfer()));

        PaymentTimelineResponse response = service.getTimeline(
                ACCOUNT_ID, RoleType.INVESTOR, PAYMENT_INTENT_ID);

        assertThat(response.entries()).anySatisfy(entry -> {
            assertThat(entry.stage()).isEqualTo(PaymentTimelineStage.PAYOUT_FAILED);
            assertThat(entry.status()).isEqualTo(PaymentTimelineStatus.FAILED);
            assertThat(entry.attemptCount()).isEqualTo(1);
        });
        assertThat(response.entries()).noneMatch(entry ->
                entry.stage() == PaymentTimelineStage.SETTLEMENT_CONFIRMED);
    }

    private static PaymentIntent confirmedIntent() {
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
                .createdAt(CREATED_AT)
                .expiresAt(CREATED_AT.plusSeconds(600))
                .confirmedAt(COLLECTION_AT)
                .build();
    }

    private static PaymentAttempt confirmedAttempt() {
        return PaymentAttempt.builder()
                .paymentAttemptId(21L)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .providerCode("DEMO")
                .methodType(PaymentMethodType.UPI)
                .attemptState(PaymentAttemptState.CONFIRMED)
                .createdAt(ATTEMPT_AT)
                .initiatedAt(ATTEMPT_AT)
                .confirmedAt(COLLECTION_AT)
                .build();
    }

    private static PayoutTransfer failedTransfer() {
        return PayoutTransfer.builder()
                .payoutTransferId(41L)
                .paymentIntentId(PAYMENT_INTENT_ID)
                .paymentAccountBindingId(51L)
                .providerCode("DEMO")
                .idempotencyKey("PAYOUT-PAYMENT-INTENT-31")
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .transferStatus(PayoutTransferStatus.FAILED)
                .attemptCount(1)
                .createdAt(COLLECTION_AT.plusSeconds(1))
                .lastAttemptAt(COLLECTION_AT.plusSeconds(2))
                .latestFailureAt(COLLECTION_AT.plusSeconds(3))
                .latestFailureCode("DEMO_PAYOUT_FAILED")
                .latestFailureMessage("Demonstration payout failed")
                .updatedAt(COLLECTION_AT.plusSeconds(3))
                .build();
    }

    private static Settlement settlement(SettlementState state, Instant confirmedAt) {
        return Settlement.builder()
                .settlementId(11L)
                .agreementId(12L)
                .startupId(13L)
                .investorId(14L)
                .amount(new BigDecimal("1250.00"))
                .currencyCode("INR")
                .settlementState(state)
                .confirmedPaymentIntentId(PAYMENT_INTENT_ID)
                .createdAt(CREATED_AT.minusSeconds(20))
                .expiresAt(CREATED_AT.plusSeconds(600))
                .confirmedAt(confirmedAt)
                .build();
    }
}
