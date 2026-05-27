package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentProviderWebhookSignatureVerifierRegistry {
    private final List<PaymentProviderWebhookSignatureVerifier> verifiers;

    public PaymentProviderWebhookSignatureVerifierRegistry(List<PaymentProviderWebhookSignatureVerifier> verifiers) {
        this.verifiers = List.copyOf(verifiers);
    }

    public PaymentProviderWebhookSignatureVerifier resolve(String providerCode) {
        String normalizedProviderCode = providerCode == null ? "" : providerCode.trim().toUpperCase();
        return verifiers.stream()
                .filter(verifier -> verifier.supports(normalizedProviderCode))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException(
                        "No webhook signature verifier configured for provider"
                ));
    }
}
