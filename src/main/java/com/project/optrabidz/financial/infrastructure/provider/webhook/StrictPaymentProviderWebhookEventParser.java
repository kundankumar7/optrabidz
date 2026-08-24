package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.request.PaymentProviderWebhookRequest;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookEventParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
public class StrictPaymentProviderWebhookEventParser implements PaymentProviderWebhookEventParser {
    private final Validator validator;
    private final ObjectReader reader;

    public StrictPaymentProviderWebhookEventParser(Validator validator) {
        this.validator = validator;
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(8)
                        .maxStringLength(512)
                        .maxNumberLength(19)
                        .build())
                .build();
        this.reader = JsonMapper.builder(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build()
                .readerFor(PaymentProviderWebhookRequest.class);
    }

    @Override
    public PaymentProviderWebhookCommand parse(String providerCode, byte[] rawBody) {
        try {
            PaymentProviderWebhookRequest request = reader.readValue(rawBody);
            validate(request);
            return request.toCommand(
                    providerCode,
                    new String(rawBody, StandardCharsets.UTF_8),
                    Map.of()
            );
        } catch (PaymentWebhookPayloadInvalidException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PaymentWebhookPayloadInvalidException("SCHEMA_INVALID", exception);
        }
    }

    private void validate(PaymentProviderWebhookRequest request) {
        if (request == null) {
            throw new PaymentWebhookPayloadInvalidException("SCHEMA_INVALID");
        }
        Set<ConstraintViolation<PaymentProviderWebhookRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new PaymentWebhookPayloadInvalidException("FIELD_INVALID");
        }
        if (request.eventType() == PaymentProviderWebhookEventType.PAYMENT_CONFIRMED
                && (request.providerPaymentId() == null || request.providerPaymentId().isBlank())) {
            throw new PaymentWebhookPayloadInvalidException("FIELD_INVALID");
        }
    }
}
