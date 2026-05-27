package com.project.optrabidz.financial.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.financial.application.PaymentProviderWebhookService;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProviderWebhookControllerTest {
    private static final String RAW_PAYLOAD = """
            {
              "eventType": "PAYMENT_CONFIRMED",
              "paymentAttemptId": 1001,
              "providerPaymentId": "UPI-PAYMENT-1001",
              "providerEventId": "evt_1001"
            }
            """;

    private final PaymentProviderWebhookService webhookService = mock(PaymentProviderWebhookService.class);
    private final PaymentProviderWebhookController controller = new PaymentProviderWebhookController(
            webhookService,
            new ObjectMapper()
    );

    @Test
    void parsesProviderWebhookWithoutLosingRawPayloadOrHeaders() {
        PaymentAttemptResponse paymentAttemptResponse = response();
        when(webhookService.handle(org.mockito.ArgumentMatchers.any(PaymentProviderWebhookCommand.class)))
                .thenReturn(paymentAttemptResponse);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-PAYMENT-SIGNATURE", "sha256=valid");

        controller.handleProviderWebhook("upi", RAW_PAYLOAD, httpRequest);

        ArgumentCaptor<PaymentProviderWebhookCommand> captor =
                ArgumentCaptor.forClass(PaymentProviderWebhookCommand.class);
        verify(webhookService).handle(captor.capture());

        PaymentProviderWebhookCommand command = captor.getValue();
        assertThat(command.providerCode()).isEqualTo("UPI");
        assertThat(command.eventType()).isEqualTo(PaymentProviderWebhookEventType.PAYMENT_CONFIRMED);
        assertThat(command.paymentAttemptId()).isEqualTo(1001L);
        assertThat(command.providerPaymentId()).isEqualTo("UPI-PAYMENT-1001");
        assertThat(command.providerEventId()).isEqualTo("evt_1001");
        assertThat(command.rawPayload()).isEqualTo(RAW_PAYLOAD);
        assertThat(command.headers()).containsEntry("X-PAYMENT-SIGNATURE", "sha256=valid");
    }

    private static PaymentAttemptResponse response() {
        return new PaymentAttemptResponse(
                1001L,
                901L,
                "UPI",
                PaymentMethodType.UPI,
                "UPI-ORDER-1001",
                "UPI-PAYMENT-1001",
                "UPI-REF-1001",
                PaymentAttemptState.CONFIRMED,
                Instant.now().minusSeconds(30),
                Instant.now().minusSeconds(20),
                Instant.now(),
                null,
                null,
                null,
                "{\"mode\":\"UPI\"}"
        );
    }
}
