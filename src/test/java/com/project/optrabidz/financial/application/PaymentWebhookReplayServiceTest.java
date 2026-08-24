package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayCollisionException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayStateException;
import com.project.optrabidz.financial.application.port.PaymentWebhookReplayStore;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayContent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayState;
import com.project.optrabidz.financial.application.replay.StoredPaymentWebhookReplayEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookReplayServiceTest {
    private static final long REPLAY_ID = 91L;
    private static final long PAYMENT_INTENT_ID = 501L;
    private static final long PAYMENT_ATTEMPT_ID = 601L;

    @Mock
    private PaymentWebhookReplayStore store;
    @Mock
    private PaymentProviderWebhookService webhookService;
    @Mock
    private PaymentAttemptResponse response;

    private PaymentWebhookReplayService service;
    private PaymentProviderWebhookCommand command;
    private PaymentWebhookReplayEvent event;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookReplayService(store, webhookService);
        command = command();
        event = event("a".repeat(64), "provider-payment-1001");
    }

    @Test
    void ownerProcessesFinancialChangeAndMarksReplayProcessed() {
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.of(REPLAY_ID));
        when(webhookService.handle(command)).thenReturn(response);
        when(response.paymentIntentId()).thenReturn(PAYMENT_INTENT_ID);
        when(response.paymentAttemptId()).thenReturn(PAYMENT_ATTEMPT_ID);

        service.handle(command, event);

        InOrder order = inOrder(store, webhookService);
        order.verify(store).tryClaim(eq(event), any(Instant.class));
        order.verify(webhookService).handle(command);
        order.verify(store).markProcessed(
                eq(REPLAY_ID),
                eq(PAYMENT_INTENT_ID),
                eq(PAYMENT_ATTEMPT_ID),
                any(Instant.class)
        );
    }

    @Test
    void identicalProcessedDuplicateSkipsFinancialProcessing() {
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.empty());
        when(store.findByIdentity("UPI", "evt-1001"))
                .thenReturn(Optional.of(new StoredPaymentWebhookReplayEvent(
                        REPLAY_ID,
                        PaymentWebhookReplayState.PROCESSED,
                        event
                )));

        service.handle(command, event);

        verifyNoInteractions(webhookService);
        verify(store, never()).markProcessed(
                anyLong(), anyLong(), anyLong(), any(Instant.class)
        );
    }

    @Test
    void processedIdentityWithDifferentContentThrowsCollision() {
        PaymentWebhookReplayEvent different = event(
                "b".repeat(64),
                "provider-payment-changed"
        );
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.empty());
        when(store.findByIdentity("UPI", "evt-1001"))
                .thenReturn(Optional.of(new StoredPaymentWebhookReplayEvent(
                        REPLAY_ID,
                        PaymentWebhookReplayState.PROCESSED,
                        different
                )));

        assertThatThrownBy(() -> service.handle(command, event))
                .isInstanceOf(PaymentWebhookReplayCollisionException.class)
                .hasMessage("Authenticated webhook event identity collision");
        verifyNoInteractions(webhookService);
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentWebhookReplayState.class,
            names = {"RECEIVED", "FAILED", "IGNORED"}
    )
    void unexpectedCommittedStateFailsClosed(
            PaymentWebhookReplayState storedState) {
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.empty());
        when(store.findByIdentity("UPI", "evt-1001"))
                .thenReturn(Optional.of(new StoredPaymentWebhookReplayEvent(
                        REPLAY_ID,
                        storedState,
                        event
                )));

        assertThatThrownBy(() -> service.handle(command, event))
                .isInstanceOf(PaymentWebhookReplayStateException.class)
                .hasMessage("Payment webhook replay state invariant failed");
        verifyNoInteractions(webhookService);
    }

    @Test
    void missingConflictingRowFailsClosed() {
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.empty());
        when(store.findByIdentity("UPI", "evt-1001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(command, event))
                .isInstanceOf(PaymentWebhookReplayStateException.class)
                .hasMessage("Payment webhook replay state invariant failed");
        verifyNoInteractions(webhookService);
    }

    @Test
    void financialFailureDoesNotMarkReplayProcessed() {
        IllegalStateException failure = new IllegalStateException(
                "financial processing failed"
        );
        when(store.tryClaim(eq(event), any(Instant.class)))
                .thenReturn(OptionalLong.of(REPLAY_ID));
        when(webhookService.handle(command)).thenThrow(failure);

        assertThatThrownBy(() -> service.handle(command, event)).isSameAs(failure);
        verify(store, never()).markProcessed(
                anyLong(), anyLong(), anyLong(), any(Instant.class)
        );
    }

    private static PaymentProviderWebhookCommand command() {
        return new PaymentProviderWebhookCommand(
                "UPI",
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                PAYMENT_ATTEMPT_ID,
                "provider-payment-1001",
                null,
                null,
                "evt-1001"
        );
    }

    private static PaymentWebhookReplayEvent event(
            String hash,
            String providerPaymentId) {
        return new PaymentWebhookReplayEvent(
                new PaymentWebhookReplayContent(
                        1,
                        "UPI",
                        "evt-1001",
                        PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                        PAYMENT_ATTEMPT_ID,
                        providerPaymentId,
                        null,
                        null
                ),
                hash
        );
    }
}
