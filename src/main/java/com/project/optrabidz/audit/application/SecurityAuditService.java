package com.project.optrabidz.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.observability.OperationalEventLogger;
import com.project.optrabidz.common.observability.RequestIdProvider;
import com.project.optrabidz.common.observability.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SecurityAuditService {
    private final AuditService auditService;
    private final AuditRecordFactory auditRecordFactory;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final ObjectMapper objectMapper;
    private final OperationalEventLogger operationalEventLogger;

    public SecurityAuditService(AuditService auditService,
                                AuditRecordFactory auditRecordFactory,
                                SensitiveDataMasker sensitiveDataMasker,
                                ObjectMapper objectMapper,
                                OperationalEventLogger operationalEventLogger) {
        this.auditService = auditService;
        this.auditRecordFactory = auditRecordFactory;
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.objectMapper = objectMapper;
        this.operationalEventLogger = operationalEventLogger;
    }

    public void recordLoginFailure(String email, String reason, HttpServletRequest request) {
        String maskedEmail = safeText(sensitiveDataMasker.maskEmail(email));
        saveSafely("LOGIN_FAILED", "CREDENTIAL", maskedEmail, null, null,
                AuditOutcome.FAILED, request, details(Map.of(
                        "email", maskedEmail,
                        "reason", safeText(reason)
                )));
    }

    public void recordAuthenticationRequired(HttpServletRequest request, String reason) {
        saveSafely("AUTHENTICATION_REQUIRED", "HTTP_REQUEST", requestPath(request), null, null,
                AuditOutcome.DENIED, request, details(Map.of(
                        "reason", safeText(reason),
                        "method", request == null ? "UNKNOWN" : request.getMethod(),
                        "path", requestPath(request)
                )));
    }

    public void recordAuthorizationDenied(HttpServletRequest request,
                                          String reason,
                                          Long actorAccountId,
                                          String actorRole) {
        saveSafely("AUTHORIZATION_DENIED", "HTTP_REQUEST", requestPath(request), actorAccountId, actorRole,
                AuditOutcome.DENIED, request, details(Map.of(
                        "reason", safeText(reason),
                        "method", request == null ? "UNKNOWN" : request.getMethod(),
                        "path", requestPath(request)
                )));
    }

    public void recordPaymentWebhookRejected(String providerCode,
                                             String requestId) {
        saveWebhookSecurityEvent(
                "PAYMENT_WEBHOOK_REJECTED",
                providerCode,
                requestId
        );
    }

    public void recordPaymentWebhookPayloadInvalid(String providerCode,
                                                   String requestId) {
        saveWebhookSecurityEvent(
                "PAYMENT_WEBHOOK_PAYLOAD_INVALID",
                providerCode,
                requestId
        );
    }

    private void saveWebhookSecurityEvent(String action,
                                          String providerCode,
                                          String requestId) {
        String safeProviderCode = boundedIdentifier(
                providerCode,
                "[A-Z0-9_-]{1,32}"
        );
        String safeRequestId = boundedIdentifier(
                requestId,
                "[A-Za-z0-9._-]{1,100}"
        );
        saveSafely(
                action,
                "PAYMENT_PROVIDER",
                safeProviderCode,
                null,
                null,
                AuditOutcome.DENIED,
                safeRequestId,
                null,
                null,
                details(Map.of("category", "PAYMENT_WEBHOOK"))
        );
    }

    private void saveSafely(String action,
                            String objectType,
                            String objectId,
                            Long actorAccountId,
                            String actorRole,
                            AuditOutcome outcome,
                            HttpServletRequest request,
                            String details) {
        saveSafely(
                action,
                objectType,
                objectId,
                actorAccountId,
                actorRole,
                outcome,
                request == null ? null : RequestIdProvider.resolveOrCreate(request),
                clientIp(request),
                userAgent(request),
                details
        );
    }

    private void saveSafely(String action,
                            String objectType,
                            String objectId,
                            Long actorAccountId,
                            String actorRole,
                            AuditOutcome outcome,
                            String requestId,
                            String ipAddress,
                            String userAgent,
                            String details) {
        try {
            auditService.save(auditRecordFactory.securityRecord(
                    action,
                    objectType,
                    objectId,
                    actorAccountId,
                    actorRole,
                    outcome,
                    requestId,
                    ipAddress,
                    userAgent,
                    details,
                    Instant.now()
            ));
        } catch (RuntimeException exception) {
            operationalEventLogger.error(
                    "SECURITY_AUDIT_WRITE_FAILED",
                    "action=" + action + " objectType=" + objectType + " objectId=" + objectId,
                    exception
            );
        }
    }

    private String boundedIdentifier(String value, String pattern) {
        return value != null && value.matches(pattern) ? value : "UNKNOWN";
    }

    private String details(Map<String, String> values) {
        try {
            Map<String, String> ordered = new LinkedHashMap<>(values);
            return sensitiveDataMasker.mask(objectMapper.writeValueAsString(ordered));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String requestPath(HttpServletRequest request) {
        return request == null ? "UNKNOWN" : request.getRequestURI();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",")[0].trim();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : sensitiveDataMasker.mask(value);
    }
}
