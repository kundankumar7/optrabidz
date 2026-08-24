package com.project.optrabidz.financial.infrastructure.provider.webhook;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrictPaymentProviderWebhookEventParserTest {
    private StrictPaymentProviderWebhookEventParser parser;

    @BeforeEach
    void setUp() {
        parser = new StrictPaymentProviderWebhookEventParser(
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    @Test
    void parsesValidConfirmedAndFailedEvents() {
        PaymentProviderWebhookCommand confirmed = parse("""
                {
                  "eventType":"PAYMENT_CONFIRMED",
                  "paymentAttemptId":1001,
                  "providerPaymentId":"UPI-PAYMENT-1001",
                  "providerEventId":"evt_1001"
                }
                """);
        PaymentProviderWebhookCommand failed = parse("""
                {
                  "eventType":"PAYMENT_FAILED",
                  "paymentAttemptId":1002,
                  "failureCode":"upi_declined",
                  "failureMessage":"Provider declined",
                  "providerEventId":"evt_1002"
                }
                """);

        assertThat(confirmed.providerCode()).isEqualTo("UPI");
        assertThat(confirmed.eventType()).isEqualTo(PaymentProviderWebhookEventType.PAYMENT_CONFIRMED);
        assertThat(confirmed.providerPaymentId()).isEqualTo("UPI-PAYMENT-1001");
        assertThat(failed.eventType()).isEqualTo(PaymentProviderWebhookEventType.PAYMENT_FAILED);
        assertThat(failed.failureCode()).isEqualTo("UPI_DECLINED");
    }

    @Test
    void rejectsDuplicateUnknownAndTrailingJsonContent() {
        assertInvalid("""
                {"eventType":"PAYMENT_FAILED","eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerPaymentId":"p","providerEventId":"e"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerPaymentId":"p","providerEventId":"e","unexpected":"value"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerPaymentId":"p","providerEventId":"e"} {"second":true}
                """);
    }

    @Test
    void rejectsInvalidTypesOverflowAndUnsupportedEvents() {
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":"1","providerPaymentId":"p","providerEventId":"e"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":999999999999999999999999,"providerPaymentId":"p","providerEventId":"e"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_REVERSED","paymentAttemptId":1,"providerPaymentId":"p","providerEventId":"e"}
                """);
    }

    @Test
    void rejectsMissingIdentifiersAndNonPositiveAttemptId() {
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":0,"providerPaymentId":"p","providerEventId":"e"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerPaymentId":"p"}
                """);
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerEventId":"e"}
                """);
    }

    @Test
    void rejectsOverlongBoundedFieldsAndExcessiveNesting() {
        assertInvalid("""
                {"eventType":"PAYMENT_FAILED","paymentAttemptId":1,"failureCode":"%s","providerEventId":"e"}
                """.formatted("C".repeat(65)));
        assertInvalid("""
                {"eventType":"PAYMENT_FAILED","paymentAttemptId":1,"failureMessage":"%s","providerEventId":"e"}
                """.formatted("M".repeat(513)));
        assertInvalid("""
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1,"providerPaymentId":"p","providerEventId":"%s"}
                """.formatted("E".repeat(129)));
        assertInvalid("[[[[[[[[[[]]]]]]]]]]");
    }

    private PaymentProviderWebhookCommand parse(String json) {
        return parser.parse("UPI", json.getBytes(StandardCharsets.UTF_8));
    }

    private void assertInvalid(String json) {
        assertThatThrownBy(() -> parse(json))
                .isInstanceOf(PaymentWebhookPayloadInvalidException.class)
                .extracting(exception -> ((PaymentWebhookPayloadInvalidException) exception).descriptor().code())
                .isEqualTo("PAYMENT_WEBHOOK_PAYLOAD_INVALID");
    }
}
