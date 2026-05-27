package com.project.optrabidz.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.observability.OperationalEventLogger;
import com.project.optrabidz.common.observability.RequestIdProvider;
import com.project.optrabidz.common.observability.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String email, String reason, HttpServletRequest request) {
        String maskedEmail = safeText(sensitiveDataMasker.maskEmail(email));
        saveSafely("LOGIN_FAILED", "CREDENTIAL", maskedEmail, null, null,
                AuditOutcome.FAILED, request, details(Map.of(
                        "email", maskedEmail,
                        "reason", safeText(reason)
                )));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthenticationRequired(HttpServletRequest request, String reason) {
        saveSafely("AUTHENTICATION_REQUIRED", "HTTP_REQUEST", requestPath(request), null, null,
                AuditOutcome.DENIED, request, details(Map.of(
                        "reason", safeText(reason),
                        "method", request == null ? "UNKNOWN" : request.getMethod(),
                        "path", requestPath(request)
                )));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    private void saveSafely(String action,
                            String objectType,
                            String objectId,
                            Long actorAccountId,
                            String actorRole,
                            AuditOutcome outcome,
                            HttpServletRequest request,
                            String details) {
        try {
            auditService.save(auditRecordFactory.securityRecord(
                    action,
                    objectType,
                    objectId,
                    actorAccountId,
                    actorRole,
                    outcome,
                    request == null ? null : RequestIdProvider.resolveOrCreate(request),
                    clientIp(request),
                    userAgent(request),
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
