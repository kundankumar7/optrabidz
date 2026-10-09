package com.project.optrabidz.common.outbox;

import com.project.optrabidz.financial.application.event.PaymentAccountBindingCreatedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingReplacedEvent;
import com.project.optrabidz.financial.application.event.PaymentCollectionConfirmedEvent;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAccountBindingOutboxMetadataTest {
    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private final OutboxEventMetadataResolver resolver = new OutboxEventMetadataResolver();

    @Test
    void bindingEventsUseStableFinancialAggregateMetadata() {
        assertThat(resolver.resolve(new PaymentAccountBindingCreatedEvent(
                11L, 501L, RoleType.STARTUP, NOW
        ))).isEqualTo(new OutboxEventMetadata("FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "11"));

        assertThat(resolver.resolve(new PaymentAccountBindingReplacedEvent(
                11L, 12L, 501L, RoleType.STARTUP, NOW
        ))).isEqualTo(new OutboxEventMetadata("FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "12"));
    }

    @Test
    void collectionConfirmationUsesPaymentIntentAggregateMetadata() {
        assertThat(resolver.resolve(new PaymentCollectionConfirmedEvent(701L, NOW)))
                .isEqualTo(new OutboxEventMetadata("FINANCIAL", "PAYMENT_INTENT", "701"));
    }
}
