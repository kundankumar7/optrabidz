package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifierRegistry;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookServiceTest {
    private static final Long PAYMENT_ATTEMPT_ID = 1001L;
    private static final Long PAYMENT_INTENT_ID = 901L;

    @Mock
    private PaymentProviderWebhookSignatureVerifier verifier;

    @Mock
    private FinancialService financialService;

    private PaymentProviderWebhookService service;

    @BeforeEach
    void setUp() {
        service = new PaymentProviderWebhookService(
                new PaymentProviderWebhookSignatureVerifierRegistry(List.of(verifier)),
                financialService
        );
    }

    @Test
    void confirmedWebhookVerifiesSignatureBeforeDelegatingToFinanceCore() {
        PaymentProviderWebhookCommand command = confirmedCommand("upi");
        PaymentAttemptResponse confirmedResponse = confirmedAttemptResponse("UPI-PAYMENT-1001");
        when(verifier.supports("UPI")).thenReturn(true);
        when(financialService.confirmProviderPaymentAttempt("UPI", PAYMENT_ATTEMPT_ID, "UPI-PAYMENT-1001"))
                .thenReturn(confirmedResponse);

        PaymentAttemptResponse response = service.handle(command);

        assertThat(response).isSameAs(confirmedResponse);
        InOrder inOrder = inOrder(verifier, financialService);
        inOrder.verify(verifier).verify(command);
        inOrder.verify(financialService)
                .confirmProviderPaymentAttempt("UPI", PAYMENT_ATTEMPT_ID, "UPI-PAYMENT-1001");
    }

    @Test
    void unsupportedProviderDoesNotTouchFinanceCore() {
        PaymentProviderWebhookCommand command = confirmedCommand("card");
        when(verifier.supports("CARD")).thenReturn(false);

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("No webhook signature verifier configured for provider");

        verify(verifier, never()).verify(command);
        verify(financialService, never()).confirmProviderPaymentAttempt("CARD", PAYMENT_ATTEMPT_ID, "UPI-PAYMENT-1001");
        verify(financialService, never()).failProviderPaymentAttempt("CARD", PAYMENT_ATTEMPT_ID, null, null);
    }

    @Test
    void failedWebhookVerifiesSignatureBeforeDelegatingToFinanceCore() {
        PaymentProviderWebhookCommand command = new PaymentProviderWebhookCommand(
                "UPI",
                PaymentProviderWebhookEventType.PAYMENT_FAILED,
                PAYMENT_ATTEMPT_ID,
                "UPI-PAYMENT-1001",
                "upi_declined",
                "UPI provider declined the payment",
                "evt_1001",
                "{\"event\":\"payment.failed\"}",
                Map.of("X-UPI-Signature", "valid-signature")
        );
        PaymentAttemptResponse failedResponse = failedAttemptResponse("UPI_DECLINED", "UPI provider declined the payment");
        when(verifier.supports("UPI")).thenReturn(true);
        when(financialService.failProviderPaymentAttempt(
                "UPI",
                PAYMENT_ATTEMPT_ID,
                "UPI_DECLINED",
                "UPI provider declined the payment"
        )).thenReturn(failedResponse);

        PaymentAttemptResponse response = service.handle(command);

        assertThat(response).isSameAs(failedResponse);
        InOrder inOrder = inOrder(verifier, financialService);
        inOrder.verify(verifier).verify(command);
        inOrder.verify(financialService).failProviderPaymentAttempt(
                "UPI",
                PAYMENT_ATTEMPT_ID,
                "UPI_DECLINED",
                "UPI provider declined the payment"
        );
        verify(financialService, never()).confirmProviderPaymentAttempt("UPI", PAYMENT_ATTEMPT_ID, "UPI-PAYMENT-1001");
    }

    private static PaymentProviderWebhookCommand confirmedCommand(String providerCode) {
        return new PaymentProviderWebhookCommand(
                providerCode,
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                PAYMENT_ATTEMPT_ID,
                "UPI-PAYMENT-1001",
                "evt_1001",
                "{\"event\":\"payment.confirmed\"}",
                Map.of("X-UPI-Signature", "valid-signature")
        );
    }

    private static PaymentAttemptResponse confirmedAttemptResponse(String providerPaymentId) {
        return new PaymentAttemptResponse(
                PAYMENT_ATTEMPT_ID,
                PAYMENT_INTENT_ID,
                "UPI",
                PaymentMethodType.UPI,
                "UPI-ORDER-1001",
                providerPaymentId,
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

    private static PaymentAttemptResponse failedAttemptResponse(String failureCode, String failureMessage) {
        return new PaymentAttemptResponse(
                PAYMENT_ATTEMPT_ID,
                PAYMENT_INTENT_ID,
                "UPI",
                PaymentMethodType.UPI,
                "UPI-ORDER-1001",
                null,
                "UPI-REF-1001",
                PaymentAttemptState.FAILED,
                Instant.now().minusSeconds(30),
                Instant.now().minusSeconds(20),
                null,
                Instant.now(),
                failureCode,
                failureMessage,
                "{\"mode\":\"UPI\"}"
        );
    }
}
