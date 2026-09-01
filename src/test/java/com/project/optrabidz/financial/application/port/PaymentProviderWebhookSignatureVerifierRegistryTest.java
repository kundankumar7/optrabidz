package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentProviderWebhookSignatureVerifierRegistryTest {
    @Test
    void resolvesSupportingVerifierUsingNormalizedProviderCode() {
        PaymentProviderWebhookSignatureVerifier verifier = mock(PaymentProviderWebhookSignatureVerifier.class);
        when(verifier.supports("UPI")).thenReturn(true);
        PaymentProviderWebhookSignatureVerifierRegistry registry =
                new PaymentProviderWebhookSignatureVerifierRegistry(List.of(verifier));

        assertThat(registry.resolve(" upi ")).isSameAs(verifier);
    }

    @Test
    void unavailableProviderUsesUniformWebhookRejection() {
        PaymentProviderWebhookSignatureVerifierRegistry registry =
                new PaymentProviderWebhookSignatureVerifierRegistry(List.of());

        assertThatThrownBy(() -> registry.resolve("unknown"))
                .isInstanceOf(PaymentWebhookRejectedException.class)
                .extracting(exception -> ((PaymentWebhookRejectedException) exception).descriptor().code())
                .isEqualTo("PAYMENT_WEBHOOK_REJECTED");
    }
}
