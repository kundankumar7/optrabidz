package com.project.optrabidz.financial.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutTransferDomainTest {
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T09:00:00Z");

    @Test
    void initialAttemptCanStartOnlyFromPending() {
        PayoutTransfer transfer = pendingTransfer();

        transfer.beginInitialAttempt(CREATED_AT.plusSeconds(10));

        assertThat(transfer.getTransferStatus()).isEqualTo(PayoutTransferStatus.PROCESSING);
        assertThat(transfer.getAttemptCount()).isEqualTo(1);
        assertThat(transfer.getLastAttemptAt()).isEqualTo(CREATED_AT.plusSeconds(10));
        assertThatThrownBy(() -> transfer.beginInitialAttempt(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failedTransferCanRetryButCannotRunAsAnotherInitialAttempt() {
        PayoutTransfer transfer = pendingTransfer();
        transfer.beginInitialAttempt(CREATED_AT.plusSeconds(10));
        transfer.markFailed("DEMO_DECLINED", "Demonstration failure", CREATED_AT.plusSeconds(20));

        assertThatThrownBy(() -> transfer.beginInitialAttempt(CREATED_AT.plusSeconds(30)))
                .isInstanceOf(IllegalStateException.class);

        transfer.beginRetry(CREATED_AT.plusSeconds(30));

        assertThat(transfer.getTransferStatus()).isEqualTo(PayoutTransferStatus.PROCESSING);
        assertThat(transfer.getAttemptCount()).isEqualTo(2);
        assertThat(transfer.getLatestFailureCode()).isEqualTo("DEMO_DECLINED");
    }

    @Test
    void confirmedTransferRetainsEarlierFailureHistory() {
        PayoutTransfer transfer = pendingTransfer();
        transfer.beginInitialAttempt(CREATED_AT.plusSeconds(10));
        transfer.markFailed("DEMO_DECLINED", "Demonstration failure", CREATED_AT.plusSeconds(20));
        transfer.beginRetry(CREATED_AT.plusSeconds(30));

        transfer.markConfirmed("demo-transfer-reference", CREATED_AT.plusSeconds(40));

        assertThat(transfer.getTransferStatus()).isEqualTo(PayoutTransferStatus.CONFIRMED);
        assertThat(transfer.getProviderTransferReference()).isEqualTo("demo-transfer-reference");
        assertThat(transfer.getConfirmedAt()).isEqualTo(CREATED_AT.plusSeconds(40));
        assertThat(transfer.getLatestFailureCode()).isEqualTo("DEMO_DECLINED");
        assertThatThrownBy(() -> transfer.beginRetry(CREATED_AT.plusSeconds(50)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retryCannotMoveTheTransferClockBackwards() {
        PayoutTransfer transfer = pendingTransfer();
        transfer.beginInitialAttempt(CREATED_AT.plusSeconds(10));
        transfer.markFailed("DEMO_DECLINED", "Demonstration failure", CREATED_AT.plusSeconds(30));

        assertThatThrownBy(() -> transfer.beginRetry(CREATED_AT.plusSeconds(20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("updatedAt");
    }

    @Test
    void reconstructedTransferRejectsAnUpdateBeforeItsLastAttempt() {
        assertThatThrownBy(() -> PayoutTransfer.builder()
                .payoutTransferId(91L)
                .paymentIntentId(71L)
                .paymentAccountBindingId(81L)
                .providerCode("DEMO")
                .idempotencyKey("payout-intent-71")
                .amount(new BigDecimal("1000.00"))
                .currencyCode("INR")
                .transferStatus(PayoutTransferStatus.PROCESSING)
                .attemptCount(1)
                .createdAt(CREATED_AT)
                .lastAttemptAt(CREATED_AT.plusSeconds(20))
                .updatedAt(CREATED_AT.plusSeconds(10))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastAttemptAt");
    }

    @Test
    void reconstructedConfirmedTransferMayRetainFailureFromAnEarlierAttempt() {
        PayoutTransfer transfer = PayoutTransfer.builder()
                .payoutTransferId(91L)
                .paymentIntentId(71L)
                .paymentAccountBindingId(81L)
                .providerCode("DEMO")
                .providerTransferReference("demo-transfer-reference")
                .idempotencyKey("payout-intent-71")
                .amount(new BigDecimal("1000.00"))
                .currencyCode("INR")
                .transferStatus(PayoutTransferStatus.CONFIRMED)
                .attemptCount(2)
                .createdAt(CREATED_AT)
                .lastAttemptAt(CREATED_AT.plusSeconds(30))
                .confirmedAt(CREATED_AT.plusSeconds(40))
                .latestFailureAt(CREATED_AT.plusSeconds(20))
                .latestFailureCode("DEMO_DECLINED")
                .updatedAt(CREATED_AT.plusSeconds(40))
                .build();

        assertThat(transfer.getLatestFailureAt()).isEqualTo(CREATED_AT.plusSeconds(20));
    }

    @Test
    void financialEnumsExposePayoutPendingStates() {
        assertThat(SettlementState.valueOf("SETTLEMENT_PAYOUT_PENDING"))
                .isEqualTo(SettlementState.SETTLEMENT_PAYOUT_PENDING);
        assertThat(RepaymentInstallmentState.valueOf("PAYOUT_PENDING"))
                .isEqualTo(RepaymentInstallmentState.PAYOUT_PENDING);
    }

    private static PayoutTransfer pendingTransfer() {
        return PayoutTransfer.create(
                71L,
                81L,
                "DEMO",
                "payout-intent-71",
                new BigDecimal("1000.00"),
                "INR",
                CREATED_AT
        );
    }
}
