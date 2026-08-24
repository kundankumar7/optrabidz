package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookServiceTest {
    @Mock
    private FinancialService financialService;
    @Mock
    private PaymentAttemptResponse response;

    private PaymentProviderWebhookService service;

    @BeforeEach
    void setUp() {
        service = new PaymentProviderWebhookService(financialService);
    }

    @Test
    void confirmedAuthenticatedEventDelegatesToConfirmationUseCase() {
        PaymentProviderWebhookCommand command = new PaymentProviderWebhookCommand(
                "UPI", PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                1001L, "UPI-PAYMENT-1001", null, null, "evt_1001");
        when(financialService.confirmProviderPaymentAttempt("UPI", 1001L, "UPI-PAYMENT-1001"))
                .thenReturn(response);

        assertThat(service.handle(command)).isSameAs(response);
        verify(financialService).confirmProviderPaymentAttempt("UPI", 1001L, "UPI-PAYMENT-1001");
    }

    @Test
    void failedAuthenticatedEventDelegatesToFailureUseCase() {
        PaymentProviderWebhookCommand command = new PaymentProviderWebhookCommand(
                "UPI", PaymentProviderWebhookEventType.PAYMENT_FAILED,
                1001L, null, "UPI_DECLINED", "Provider declined", "evt_1001");
        when(financialService.failProviderPaymentAttempt(
                "UPI", 1001L, "UPI_DECLINED", "Provider declined"))
                .thenReturn(response);

        assertThat(service.handle(command)).isSameAs(response);
        verify(financialService).failProviderPaymentAttempt(
                "UPI", 1001L, "UPI_DECLINED", "Provider declined");
    }
}
