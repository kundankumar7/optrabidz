package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacPaymentProviderWebhookSignatureVerifierTest {
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
    private static final String ACTIVE_SECRET = "active-webhook-secret-material-0001";
    private static final String PREVIOUS_SECRET = "previous-webhook-secret-material-001";
    private static final byte[] BODY = "{\"eventType\":\"PAYMENT_CONFIRMED\"}"
            .getBytes(StandardCharsets.UTF_8);

    private PaymentWebhookProperties properties;
    private HmacPaymentProviderWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = properties(ACTIVE_SECRET, null, null);
        verifier = verifier(properties);
    }

    @Test
    void supportsOnlyEnabledConfiguredProviders() {
        assertThat(verifier.supports("UPI")).isTrue();
        assertThat(verifier.supports("CARD")).isFalse();
    }

    @Test
    void acceptsFreshSignatureFromActiveSecret() {
        String timestamp = Long.toString(NOW.getEpochSecond());

        assertThatCode(() -> verifier.verify(envelope(timestamp, BODY, ACTIVE_SECRET)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAlteredExactBodyBytes() {
        String timestamp = Long.toString(NOW.getEpochSecond());
        PaymentProviderWebhookEnvelope signed = envelope(timestamp, BODY, ACTIVE_SECRET);
        byte[] altered = BODY.clone();
        altered[altered.length - 2] ^= 1;

        assertRejected(new PaymentProviderWebhookEnvelope(
                "UPI", altered, timestamp, signed.signature()));
    }

    @Test
    void rejectsMalformedStaleAndFutureTimestamps() {
        assertRejected(envelope("not-epoch-seconds", BODY, ACTIVE_SECRET));
        assertRejected(envelope(Long.toString(NOW.minusSeconds(301).getEpochSecond()), BODY, ACTIVE_SECRET));
        assertRejected(envelope(Long.toString(NOW.plusSeconds(301).getEpochSecond()), BODY, ACTIVE_SECRET));
    }

    @Test
    void rejectsMalformedOrIncorrectSignature() {
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertRejected(new PaymentProviderWebhookEnvelope("UPI", BODY, timestamp, "sha256=xyz"));
        assertRejected(new PaymentProviderWebhookEnvelope("UPI", BODY, timestamp, "v1=" + "a".repeat(64)));
        assertRejected(new PaymentProviderWebhookEnvelope("UPI", BODY, timestamp, "sha256=" + "a".repeat(64)));
    }

    @Test
    void acceptsPreviousSecretOnlyBeforeItsExpiry() {
        String timestamp = Long.toString(NOW.getEpochSecond());
        properties = properties(ACTIVE_SECRET, PREVIOUS_SECRET, NOW.plusSeconds(60));
        verifier = verifier(properties);
        assertThatCode(() -> verifier.verify(envelope(timestamp, BODY, PREVIOUS_SECRET)))
                .doesNotThrowAnyException();

        properties = properties(ACTIVE_SECRET, PREVIOUS_SECRET, NOW);
        verifier = verifier(properties);
        assertRejected(envelope(timestamp, BODY, PREVIOUS_SECRET));
    }

    private static HmacPaymentProviderWebhookSignatureVerifier verifier(
            PaymentWebhookProperties properties) {
        return new HmacPaymentProviderWebhookSignatureVerifier(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static PaymentWebhookProperties properties(
            String activeSecret,
            String previousSecret,
            Instant previousExpiry) {
        PaymentWebhookProperties.ProviderConfiguration provider =
                new PaymentWebhookProperties.ProviderConfiguration();
        provider.setEnabled(true);
        provider.setActiveSecret(activeSecret);
        provider.setPreviousSecret(previousSecret);
        provider.setPreviousSecretValidUntil(previousExpiry);
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        properties.setTimestampTolerance(Duration.ofMinutes(5));
        properties.setProviders(Map.of("UPI", provider));
        return properties;
    }

    private static PaymentProviderWebhookEnvelope envelope(
            String timestamp,
            byte[] body,
            String secret) {
        return new PaymentProviderWebhookEnvelope(
                "UPI",
                body,
                timestamp,
                "sha256=" + hmac(timestamp, body, secret)
        );
    }

    private static String hmac(String timestamp, byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void assertRejected(PaymentProviderWebhookEnvelope envelope) {
        assertThatThrownBy(() -> verifier.verify(envelope))
                .isInstanceOf(PaymentWebhookRejectedException.class)
                .extracting(exception -> ((PaymentWebhookRejectedException) exception).descriptor().code())
                .isEqualTo("PAYMENT_WEBHOOK_REJECTED");
    }
}
