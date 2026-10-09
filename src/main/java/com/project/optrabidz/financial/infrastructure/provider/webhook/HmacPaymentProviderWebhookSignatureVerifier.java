package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class HmacPaymentProviderWebhookSignatureVerifier implements PaymentProviderWebhookSignatureVerifier {
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final PaymentWebhookProperties properties;
    private final Clock clock;
    private final PaymentWebhookHmac hmac;

    @Autowired
    public HmacPaymentProviderWebhookSignatureVerifier(PaymentWebhookProperties properties) {
        this(properties, Clock.systemUTC(), new PaymentWebhookHmac());
    }

    HmacPaymentProviderWebhookSignatureVerifier(PaymentWebhookProperties properties, Clock clock) {
        this(properties, clock, new PaymentWebhookHmac());
    }

    HmacPaymentProviderWebhookSignatureVerifier(PaymentWebhookProperties properties,
                                                Clock clock,
                                                PaymentWebhookHmac hmac) {
        this.properties = properties;
        this.clock = clock;
        this.hmac = hmac;
    }

    @Override
    public boolean supports(String providerCode) {
        return properties.enabledProvider(providerCode).isPresent();
    }

    @Override
    public void verify(PaymentProviderWebhookEnvelope envelope) {
        PaymentWebhookProperties.ProviderConfiguration provider = properties
                .enabledProvider(envelope.providerCode())
                .orElseThrow(() -> rejected(PaymentWebhookRejectionReason.PROVIDER_UNAVAILABLE));
        Instant signedAt = parseTimestamp(envelope.timestamp());
        verifyFreshness(signedAt);
        byte[] actualSignature = parseSignature(envelope.signature());
        byte[] activeCandidate = hmac.calculate(
                envelope.timestamp(), envelope.rawBody(), provider.getActiveSecret());
        boolean activeMatch = MessageDigest.isEqual(activeCandidate, actualSignature);

        boolean previousMatch = false;
        if (provider.getPreviousSecret() != null
                && !provider.getPreviousSecret().isBlank()
                && provider.getPreviousSecretValidUntil() != null
                && clock.instant().isBefore(provider.getPreviousSecretValidUntil())) {
            byte[] previousCandidate = hmac.calculate(
                    envelope.timestamp(), envelope.rawBody(), provider.getPreviousSecret());
            previousMatch = MessageDigest.isEqual(previousCandidate, actualSignature);
        }

        if (!(activeMatch | previousMatch)) {
            throw rejected(PaymentWebhookRejectionReason.SIGNATURE_INVALID);
        }
    }

    private Instant parseTimestamp(String timestamp) {
        if (timestamp == null || !timestamp.matches("[0-9]{1,20}")) {
            throw rejected(PaymentWebhookRejectionReason.TIMESTAMP_INVALID);
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (RuntimeException exception) {
            throw new PaymentWebhookRejectedException(
                    PaymentWebhookRejectionReason.TIMESTAMP_INVALID,
                    exception
            );
        }
    }

    private void verifyFreshness(Instant signedAt) {
        Instant now = clock.instant();
        if (signedAt.isBefore(now.minus(properties.getTimestampTolerance()))
                || signedAt.isAfter(now.plus(properties.getTimestampTolerance()))) {
            throw rejected(PaymentWebhookRejectionReason.TIMESTAMP_STALE);
        }
    }

    private byte[] parseSignature(String signature) {
        if (signature == null
                || !signature.startsWith(SIGNATURE_PREFIX)
                || signature.length() != SIGNATURE_PREFIX.length() + 64) {
            throw rejected(PaymentWebhookRejectionReason.SIGNATURE_INVALID);
        }
        String hexadecimal = signature.substring(SIGNATURE_PREFIX.length());
        if (!hexadecimal.matches("[0-9a-fA-F]{64}")) {
            throw rejected(PaymentWebhookRejectionReason.SIGNATURE_INVALID);
        }
        return HexFormat.of().parseHex(hexadecimal);
    }

    private PaymentWebhookRejectedException rejected(PaymentWebhookRejectionReason reason) {
        return new PaymentWebhookRejectedException(reason);
    }
}
