package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.exception.PaymentWebhookVerificationException;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Component
public class HmacPaymentProviderWebhookSignatureVerifier implements PaymentProviderWebhookSignatureVerifier {
    private static final String SIGNATURE_HEADER = "X-PAYMENT-SIGNATURE";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final PaymentWebhookProperties properties;

    public HmacPaymentProviderWebhookSignatureVerifier(PaymentWebhookProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String providerCode) {
        return properties.secretForProvider(providerCode).isPresent();
    }

    @Override
    public void verify(PaymentProviderWebhookCommand command) {
        String secret = properties.secretForProvider(command.providerCode())
                .orElseThrow(() -> new PaymentWebhookVerificationException("Webhook provider is not configured"));
        String actualSignature = headerValue(command.headers(), SIGNATURE_HEADER);
        if (actualSignature == null || actualSignature.isBlank()) {
            throw new PaymentWebhookVerificationException("Webhook signature is missing");
        }

        String expectedSignature = SIGNATURE_PREFIX + hmacSha256(command.rawPayload(), secret);
        if (!constantTimeEquals(expectedSignature, normalizeSignature(actualSignature))) {
            throw new PaymentWebhookVerificationException("Webhook signature verification failed");
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new PaymentWebhookVerificationException("Webhook signature verification failed");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String normalizeSignature(String signature) {
        String normalized = signature.trim().toLowerCase();
        return normalized.startsWith(SIGNATURE_PREFIX) ? normalized : SIGNATURE_PREFIX + normalized;
    }

    private String headerValue(Map<String, String> headers, String name) {
        return headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
