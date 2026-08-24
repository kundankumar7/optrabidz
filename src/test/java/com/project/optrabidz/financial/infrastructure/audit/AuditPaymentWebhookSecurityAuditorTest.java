package com.project.optrabidz.financial.infrastructure.audit;

import com.project.optrabidz.audit.application.SecurityAuditService;
import com.project.optrabidz.financial.infrastructure.provider.webhook.PaymentWebhookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditPaymentWebhookSecurityAuditorTest {
    private SecurityAuditService securityAuditService;
    private AuditPaymentWebhookSecurityAuditor auditor;

    @BeforeEach
    void setUp() {
        securityAuditService = mock(SecurityAuditService.class);
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        PaymentWebhookProperties.ProviderConfiguration upi =
                new PaymentWebhookProperties.ProviderConfiguration();
        upi.setEnabled(true);
        upi.setActiveSecret("test-only-upi-webhook-secret-material-001");
        properties.setProviders(Map.of("UPI", upi));
        auditor = new AuditPaymentWebhookSecurityAuditor(
                securityAuditService,
                properties
        );
    }

    @Test
    void recordsOnlyNormalizedConfiguredProviderAndRequestId() {
        auditor.recordRejected(" upi ", "request-123");

        verify(securityAuditService).recordPaymentWebhookRejected(
                "UPI",
                "request-123"
        );
    }

    @Test
    void replacesUnknownOrUnsafeProviderTextBeforeAuditing() {
        auditor.recordPayloadInvalid("unknown-provider-secret", "request-456");

        verify(securityAuditService).recordPaymentWebhookPayloadInvalid(
                "UNKNOWN",
                "request-456"
        );
    }
}
