package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class PaymentWebhookHttpRequestReader {
    static final String TIMESTAMP_HEADER = "X-Payment-Timestamp";
    static final String SIGNATURE_HEADER = "X-Payment-Signature";
    private static final int PROVIDER_CODE_MAX_LENGTH = 32;
    private static final int TIMESTAMP_MAX_LENGTH = 20;
    private static final int SIGNATURE_MAX_LENGTH = 80;

    private final PaymentWebhookProperties properties;

    public PaymentWebhookHttpRequestReader(PaymentWebhookProperties properties) {
        this.properties = properties;
    }

    public PaymentProviderWebhookEnvelope read(String providerCode, HttpServletRequest request) {
        String normalizedProviderCode = normalizeProviderCode(providerCode);
        int maximumBodyBytes = Math.toIntExact(properties.getMaxBodySize().toBytes());
        if (request.getContentLengthLong() > maximumBodyBytes) {
            throw rejected(PaymentWebhookRejectionReason.BODY_TOO_LARGE);
        }

        byte[] body = readBody(request, maximumBodyBytes);
        String timestamp = singleHeader(request, TIMESTAMP_HEADER, TIMESTAMP_MAX_LENGTH);
        String signature = singleHeader(request, SIGNATURE_HEADER, SIGNATURE_MAX_LENGTH);
        return new PaymentProviderWebhookEnvelope(normalizedProviderCode, body, timestamp, signature);
    }

    private byte[] readBody(HttpServletRequest request, int maximumBodyBytes) {
        try {
            byte[] body = request.getInputStream().readNBytes(maximumBodyBytes + 1);
            if (body.length == 0) {
                throw rejected(PaymentWebhookRejectionReason.BODY_INVALID);
            }
            if (body.length > maximumBodyBytes) {
                throw rejected(PaymentWebhookRejectionReason.BODY_TOO_LARGE);
            }
            return body;
        } catch (PaymentWebhookRejectedException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PaymentWebhookRejectedException(
                    PaymentWebhookRejectionReason.BODY_INVALID,
                    exception
            );
        }
    }

    private String singleHeader(HttpServletRequest request, String name, int maximumLength) {
        List<String> values = Collections.list(request.getHeaders(name));
        if (values.size() != 1) {
            throw rejected(PaymentWebhookRejectionReason.AUTHENTICATION_ENVELOPE_INVALID);
        }
        String value = values.getFirst();
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw rejected(PaymentWebhookRejectionReason.AUTHENTICATION_ENVELOPE_INVALID);
        }
        return value;
    }

    private String normalizeProviderCode(String providerCode) {
        String normalized = providerCode == null
                ? ""
                : providerCode.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()
                || normalized.length() > PROVIDER_CODE_MAX_LENGTH
                || !normalized.matches("[A-Z0-9_-]+")) {
            throw rejected(PaymentWebhookRejectionReason.PROVIDER_CODE_INVALID);
        }
        return normalized;
    }

    private PaymentWebhookRejectedException rejected(PaymentWebhookRejectionReason reason) {
        return new PaymentWebhookRejectedException(reason);
    }
}
