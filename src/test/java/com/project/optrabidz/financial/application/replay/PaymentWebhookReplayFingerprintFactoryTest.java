package com.project.optrabidz.financial.application.replay;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentWebhookReplayFingerprintFactoryTest {
    private PaymentWebhookReplayFingerprintFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PaymentWebhookReplayFingerprintFactory();
    }

    @Test
    void identicalNormalizedCommandsProduceTheSameVersionOneHash() {
        PaymentWebhookReplayEvent first = factory.create(confirmedCommand(
                "UPI",
                1001L,
                "evt-1001",
                "provider-payment-1001"
        ));
        PaymentWebhookReplayEvent second = factory.create(confirmedCommand(
                " upi ",
                1001L,
                "evt-1001",
                " provider-payment-1001 "
        ));

        assertThat(first).isEqualTo(second);
        assertThat(first.content().fingerprintVersion()).isEqualTo(1);
        assertThat(first.payloadHash()).matches("[0-9a-f]{64}");
    }

    @Test
    void changingAnyConfirmationIdentityFieldChangesTheReplayEvent() {
        PaymentWebhookReplayEvent baseline = factory.create(confirmedCommand(
                "UPI",
                1001L,
                "evt-1001",
                "provider-payment-1001"
        ));

        assertThat(factory.create(confirmedCommand(
                "CARD", 1001L, "evt-1001", "provider-payment-1001"
        ))).isNotEqualTo(baseline);
        assertThat(factory.create(confirmedCommand(
                "UPI", 1002L, "evt-1001", "provider-payment-1001"
        ))).isNotEqualTo(baseline);
        assertThat(factory.create(confirmedCommand(
                "UPI", 1001L, "evt-1002", "provider-payment-1001"
        ))).isNotEqualTo(baseline);
        assertThat(factory.create(confirmedCommand(
                "UPI", 1001L, "evt-1001", "provider-payment-1002"
        ))).isNotEqualTo(baseline);
    }

    @Test
    void changingEventTypeChangesTheReplayEvent() {
        PaymentWebhookReplayEvent confirmed = factory.create(confirmedCommand(
                "UPI", 1001L, "evt-1001", "provider-payment-1001"
        ));
        PaymentWebhookReplayEvent failed = factory.create(failedCommand(
                "UPI", 1001L, "evt-1001", "UPI_DECLINED", "Provider declined"
        ));

        assertThat(failed).isNotEqualTo(confirmed);
    }

    @Test
    void providerFailureDiagnosticsAreNormalizedAndFingerprintProtected() {
        PaymentWebhookReplayEvent first = factory.create(failedCommand(
                "UPI", 1001L, "evt-1001", "upi_declined", " Provider declined "
        ));
        PaymentWebhookReplayEvent normalized = factory.create(failedCommand(
                "UPI", 1001L, "evt-1001", "UPI_DECLINED", "Provider declined"
        ));
        PaymentWebhookReplayEvent changedCode = factory.create(failedCommand(
                "UPI", 1001L, "evt-1001", "BANK_DECLINED", "Provider declined"
        ));
        PaymentWebhookReplayEvent changedMessage = factory.create(failedCommand(
                "UPI", 1001L, "evt-1001", "UPI_DECLINED", "Provider declined after retry"
        ));

        assertThat(first).isEqualTo(normalized);
        assertThat(changedCode).isNotEqualTo(first);
        assertThat(changedMessage).isNotEqualTo(first);
        assertThat(first.content().providerFailureCode()).isEqualTo("UPI_DECLINED");
        assertThat(first.content().providerFailureMessage()).isEqualTo("Provider declined");
    }

    private static PaymentProviderWebhookCommand confirmedCommand(
            String providerCode,
            Long paymentAttemptId,
            String providerEventId,
            String providerPaymentId) {
        return new PaymentProviderWebhookCommand(
                providerCode,
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                paymentAttemptId,
                providerPaymentId,
                null,
                null,
                providerEventId
        );
    }

    private static PaymentProviderWebhookCommand failedCommand(
            String providerCode,
            Long paymentAttemptId,
            String providerEventId,
            String failureCode,
            String failureMessage) {
        return new PaymentProviderWebhookCommand(
                providerCode,
                PaymentProviderWebhookEventType.PAYMENT_FAILED,
                paymentAttemptId,
                null,
                failureCode,
                failureMessage,
                providerEventId
        );
    }
}
