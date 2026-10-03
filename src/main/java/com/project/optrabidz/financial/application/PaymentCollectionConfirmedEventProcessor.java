package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.common.outbox.OutboxEventProcessor;
import com.project.optrabidz.financial.application.event.PaymentCollectionConfirmedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentCollectionConfirmedEventProcessor implements OutboxEventProcessor {
    private static final String EVENT_TYPE = PaymentCollectionConfirmedEvent.class.getSimpleName();

    private final PayoutService payoutService;
    private final ObjectMapper objectMapper;

    public PaymentCollectionConfirmedEventProcessor(PayoutService payoutService,
                                                    ObjectMapper objectMapper) {
        this.payoutService = payoutService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return EVENT_TYPE.equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        PaymentCollectionConfirmedEvent confirmedEvent = deserialize(event.getPayload());
        PayoutTransferEnsureResult result = payoutService.ensureTransfer(
                confirmedEvent.paymentIntentId());
        if (result.created()) {
            payoutService.executeInitial(result.transfer().getPayoutTransferId());
        }
    }

    private PaymentCollectionConfirmedEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentCollectionConfirmedEvent.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Payment collection confirmation event could not be deserialized",
                    exception
            );
        }
    }
}
