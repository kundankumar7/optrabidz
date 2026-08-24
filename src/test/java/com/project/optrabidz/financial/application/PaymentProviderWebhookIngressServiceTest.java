package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookEventParser;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifierRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderWebhookIngressServiceTest {
    private static final byte[] BODY = "{\"eventType\":\"PAYMENT_CONFIRMED\"}"
            .getBytes(StandardCharsets.UTF_8);

    @Mock
    private PaymentProviderWebhookSignatureVerifier verifier;
    @Mock
    private PaymentProviderWebhookEventParser parser;
    @Mock
    private PaymentProviderWebhookService webhookService;
    @Mock
    private PaymentAttemptResponse response;

    private PaymentProviderWebhookIngressService ingressService;

    @BeforeEach
    void setUp() {
        when(verifier.supports("UPI")).thenReturn(true);
        ingressService = new PaymentProviderWebhookIngressService(
                new PaymentProviderWebhookSignatureVerifierRegistry(List.of(verifier)),
                parser,
                webhookService
        );
    }

    @Test
    void verifiesBeforeParsingAndFinancialProcessing() {
        PaymentProviderWebhookEnvelope envelope = envelope();
        PaymentProviderWebhookCommand command = command();
        when(parser.parse(eq("UPI"), aryEq(BODY))).thenReturn(command);
        when(webhookService.handle(command)).thenReturn(response);

        assertThat(ingressService.handle(envelope)).isSameAs(response);

        InOrder order = inOrder(verifier, parser, webhookService);
        order.verify(verifier).verify(envelope);
        order.verify(parser).parse(eq("UPI"), aryEq(BODY));
        order.verify(webhookService).handle(command);
    }

    @Test
    void verificationFailurePreventsParsingAndFinancialProcessing() {
        PaymentProviderWebhookEnvelope envelope = envelope();
        PaymentWebhookRejectedException rejected = new PaymentWebhookRejectedException(
                PaymentWebhookRejectionReason.SIGNATURE_INVALID
        );
        org.mockito.Mockito.doThrow(rejected).when(verifier).verify(envelope);

        assertThatThrownBy(() -> ingressService.handle(envelope)).isSameAs(rejected);
        verify(parser, never()).parse(eq("UPI"), org.mockito.ArgumentMatchers.any(byte[].class));
        verify(webhookService, never()).handle(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void parsingFailurePreventsFinancialProcessing() {
        PaymentProviderWebhookEnvelope envelope = envelope();
        PaymentWebhookPayloadInvalidException invalid =
                new PaymentWebhookPayloadInvalidException("SCHEMA_INVALID");
        when(parser.parse(eq("UPI"), aryEq(BODY))).thenThrow(invalid);

        assertThatThrownBy(() -> ingressService.handle(envelope)).isSameAs(invalid);
        verify(webhookService, never()).handle(org.mockito.ArgumentMatchers.any());
    }

    private static PaymentProviderWebhookEnvelope envelope() {
        return new PaymentProviderWebhookEnvelope(
                "UPI", BODY, "1787553600", "sha256=" + "a".repeat(64));
    }

    private static PaymentProviderWebhookCommand command() {
        return new PaymentProviderWebhookCommand(
                "UPI",
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                1001L,
                "UPI-PAYMENT-1001",
                null,
                null,
                "evt_1001"
        );
    }
}
