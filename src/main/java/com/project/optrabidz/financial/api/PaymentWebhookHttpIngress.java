package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.observability.RequestIdProvider;
import com.project.optrabidz.financial.application.PaymentProviderWebhookIngressService;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.port.PaymentWebhookSecurityAuditor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookHttpIngress {
    private final PaymentWebhookHttpRequestReader requestReader;
    private final PaymentProviderWebhookIngressService ingressService;
    private final PaymentWebhookSecurityAuditor securityAuditor;

    public PaymentWebhookHttpIngress(PaymentWebhookHttpRequestReader requestReader,
                                     PaymentProviderWebhookIngressService ingressService,
                                     PaymentWebhookSecurityAuditor securityAuditor) {
        this.requestReader = requestReader;
        this.ingressService = ingressService;
        this.securityAuditor = securityAuditor;
    }

    public PaymentAttemptResponse handle(String providerCode, HttpServletRequest request) {
        String requestId = RequestIdProvider.resolveOrCreate(request);
        String auditProviderCode = providerCode;
        try {
            PaymentProviderWebhookEnvelope envelope = requestReader.read(providerCode, request);
            auditProviderCode = envelope.providerCode();
            return ingressService.handle(envelope);
        } catch (PaymentWebhookRejectedException exception) {
            securityAuditor.recordRejected(auditProviderCode, requestId);
            throw exception;
        } catch (PaymentWebhookPayloadInvalidException exception) {
            securityAuditor.recordPayloadInvalid(auditProviderCode, requestId);
            throw exception;
        }
    }
}
