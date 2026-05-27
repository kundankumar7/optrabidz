package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.exception.PaymentWebhookVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacPaymentProviderWebhookSignatureVerifierTest {
    private static final String SECRET = "test-webhook-secret";
    private static final String RAW_PAYLOAD = "{\"eventType\":\"PAYMENT_CONFIRMED\",\"paymentAttemptId\":1001}";

    private HmacPaymentProviderWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        properties.setHmacSecrets(Map.of("upi", SECRET));
        verifier = new HmacPaymentProviderWebhookSignatureVerifier(properties);
    }

    @Test
    void supportsOnlyProvidersWithConfiguredSecret() {
        assertThat(verifier.supports("UPI")).isTrue();
        assertThat(verifier.supports("CARD")).isFalse();
    }

    @Test
    void acceptsValidHmacSignature() {
        PaymentProviderWebhookCommand command = commandWithSignature("sha256=" + hmac(RAW_PAYLOAD, SECRET));

        verifier.verify(command);
    }

    @Test
    void rejectsMissingSignature() {
        PaymentProviderWebhookCommand command = command(Map.of());

        assertThatThrownBy(() -> verifier.verify(command))
                .isInstanceOf(PaymentWebhookVerificationException.class)
                .hasMessage("Webhook signature is missing");
    }

    @Test
    void rejectsInvalidSignature() {
        PaymentProviderWebhookCommand command = commandWithSignature("sha256=bad-signature");

        assertThatThrownBy(() -> verifier.verify(command))
                .isInstanceOf(PaymentWebhookVerificationException.class)
                .hasMessage("Webhook signature verification failed");
    }

    private PaymentProviderWebhookCommand commandWithSignature(String signature) {
        return command(Map.of("X-PAYMENT-SIGNATURE", signature));
    }

    private PaymentProviderWebhookCommand command(Map<String, String> headers) {
        return new PaymentProviderWebhookCommand(
                "UPI",
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                1001L,
                "UPI-PAYMENT-1001",
                "evt_1001",
                RAW_PAYLOAD,
                headers
        );
    }

    private static String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
