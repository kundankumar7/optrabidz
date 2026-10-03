package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCollectionConfirmedEventProcessorTest {
    private static final Long PAYMENT_INTENT_ID = 31L;
    private static final Long PAYOUT_TRANSFER_ID = 41L;

    @Mock private PayoutService payoutService;
    @Mock private OutboxEvent outboxEvent;
    @Mock private PayoutTransfer payoutTransfer;

    private PaymentCollectionConfirmedEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PaymentCollectionConfirmedEventProcessor(
                payoutService, JsonMapper.builder().build());
    }

    @Test
    void supportsOnlyCollectionConfirmedEvents() {
        when(outboxEvent.getEventType())
                .thenReturn("PaymentCollectionConfirmedEvent", "SettlementConfirmedEvent");

        assertThat(processor.supports(outboxEvent)).isTrue();
        assertThat(processor.supports(outboxEvent)).isFalse();
    }

    @Test
    void createsAndExecutesNewTransfer() {
        when(outboxEvent.getPayload()).thenReturn(
                "{\"paymentIntentId\":31,\"occurredAt\":\"2026-10-02T10:00:00Z\"}");
        when(payoutTransfer.getPayoutTransferId()).thenReturn(PAYOUT_TRANSFER_ID);
        when(payoutService.ensureTransfer(PAYMENT_INTENT_ID))
                .thenReturn(new PayoutTransferEnsureResult(payoutTransfer, true));

        processor.process(outboxEvent);

        verify(payoutService).executeInitial(PAYOUT_TRANSFER_ID);
    }

    @Test
    void duplicateDeliveryDoesNotExecuteExistingTransfer() {
        when(outboxEvent.getPayload()).thenReturn(
                "{\"paymentIntentId\":31,\"occurredAt\":\"2026-10-02T10:00:00Z\"}");
        when(payoutService.ensureTransfer(PAYMENT_INTENT_ID))
                .thenReturn(new PayoutTransferEnsureResult(payoutTransfer, false));

        processor.process(outboxEvent);

        verify(payoutService, never()).executeInitial(PAYOUT_TRANSFER_ID);
    }
}
