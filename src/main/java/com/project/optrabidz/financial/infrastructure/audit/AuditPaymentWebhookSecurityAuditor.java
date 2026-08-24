package com.project.optrabidz.financial.infrastructure.audit;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.financial.application.port.PaymentWebhookSecurityAuditor;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuditPaymentWebhookSecurityAuditor
        implements PaymentWebhookSecurityAuditor {
    private final SecurityAuditService securityAuditService;
    private final PaymentWebhookProperties properties;

    public AuditPaymentWebhookSecurityAuditor(
            SecurityAuditService securityAuditService,
            PaymentWebhookProperties properties) {
        this.securityAuditService = securityAuditService;
        this.properties = properties;
    }

    @Override
    public void recordRejected(String providerCode, String requestId) {
        securityAuditService.recordPaymentWebhookRejected(
                safeProviderCode(providerCode),
                requestId
        );
    }

    @Override
    public void recordPayloadInvalid(String providerCode, String requestId) {
        securityAuditService.recordPaymentWebhookPayloadInvalid(
                safeProviderCode(providerCode),
                requestId
        );
    }

    private String safeProviderCode(String providerCode) {
        if (providerCode == null) {
            return "UNKNOWN";
        }
        String normalized = providerCode.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{1,32}")) {
            return "UNKNOWN";
        }
        return properties.enabledProvider(normalized).isPresent()
                ? normalized
                : "UNKNOWN";
    }
}
