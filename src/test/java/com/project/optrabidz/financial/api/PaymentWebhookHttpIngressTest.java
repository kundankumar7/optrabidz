package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.PaymentProviderWebhookIngressService;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayCollisionException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import com.project.optrabidz.financial.application.port.PaymentWebhookSecurityAuditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookHttpIngressTest {
    private static final String REQUEST_ID = "kan-36-request";

    @Mock
    private PaymentWebhookHttpRequestReader reader;
    @Mock
    private PaymentProviderWebhookIngressService ingressService;
    @Mock
    private PaymentWebhookSecurityAuditor auditor;

    private PaymentWebhookHttpIngress ingress;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ingress = new PaymentWebhookHttpIngress(reader, ingressService, auditor);
        request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", REQUEST_ID);
    }

    @Test
    void readerRejectionIsAuditedOnceAndRethrown() {
        PaymentWebhookRejectedException rejection = new PaymentWebhookRejectedException(
                PaymentWebhookRejectionReason.BODY_TOO_LARGE);
        when(reader.read("upi", request)).thenThrow(rejection);

        assertThatThrownBy(() -> ingress.handle("upi", request)).isSameAs(rejection);
        verify(auditor).recordRejected("upi", REQUEST_ID);
        verify(auditor, never()).recordPayloadInvalid("upi", REQUEST_ID);
    }

    @Test
    void authenticatedPayloadFailureIsAuditedOnceAndRethrown() {
        PaymentProviderWebhookEnvelope envelope = new PaymentProviderWebhookEnvelope(
                "UPI", new byte[]{'{' , '}'}, "1787553600", "sha256=" + "a".repeat(64));
        PaymentWebhookPayloadInvalidException invalid =
                new PaymentWebhookPayloadInvalidException("SCHEMA_INVALID");
        when(reader.read("upi", request)).thenReturn(envelope);
        org.mockito.Mockito.doThrow(invalid).when(ingressService).handle(envelope);

        assertThatThrownBy(() -> ingress.handle("upi", request)).isSameAs(invalid);
        verify(auditor).recordPayloadInvalid("UPI", REQUEST_ID);
        verify(auditor, never()).recordRejected("UPI", REQUEST_ID);
    }

    @Test
    void replayCollisionIsAuditedSeparatelyAndRethrown() {
        PaymentProviderWebhookEnvelope envelope = new PaymentProviderWebhookEnvelope(
                "UPI", new byte[]{'{' , '}'}, "1787553600", "sha256=" + "a".repeat(64));
        PaymentWebhookReplayCollisionException collision =
                new PaymentWebhookReplayCollisionException();
        when(reader.read("upi", request)).thenReturn(envelope);
        org.mockito.Mockito.doThrow(collision).when(ingressService).handle(envelope);

        assertThatThrownBy(() -> ingress.handle("upi", request)).isSameAs(collision);
        verify(auditor).recordReplayCollision("UPI", REQUEST_ID);
        verify(auditor, never()).recordPayloadInvalid("UPI", REQUEST_ID);
        verify(auditor, never()).recordRejected("UPI", REQUEST_ID);
    }
}
