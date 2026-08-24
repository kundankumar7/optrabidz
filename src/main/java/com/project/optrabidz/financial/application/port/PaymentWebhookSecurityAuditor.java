package com.project.optrabidz.financial.application.port;

public interface PaymentWebhookSecurityAuditor {
    void recordRejected(String providerCode, String requestId);

    void recordPayloadInvalid(String providerCode, String requestId);
}
