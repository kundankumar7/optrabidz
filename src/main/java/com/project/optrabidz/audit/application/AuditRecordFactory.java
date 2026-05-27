package com.project.optrabidz.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.application.policy.AuditDescriptor;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.audit.infrastructure.entity.AuditRecord;
import com.project.optrabidz.common.observability.SensitiveDataMasker;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Component
public class AuditRecordFactory {
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker sensitiveDataMasker;

    public AuditRecordFactory(ObjectMapper objectMapper,
                              SensitiveDataMasker sensitiveDataMasker) {
        this.objectMapper = objectMapper;
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    public AuditRecord fromOutboxSuccess(OutboxEvent event, AuditDescriptor descriptor) {
        AuditRecord auditRecord = baseRecord(
                event.getEventId(),
                event.getEventType(),
                normalizeSourceModule(defaultString(descriptor.sourceModule(), event.getSourceModule())),
                descriptor.action(),
                defaultString(descriptor.objectType(), event.getAggregateType()),
                defaultString(descriptor.objectId(), event.getAggregateId()),
                descriptor.outcome(),
                safeJson(descriptor.details()),
                event.getOccurredAt()
        );
        auditRecord.setActorAccountId(descriptor.actorAccountId());
        auditRecord.setActorRole(descriptor.actorRole());
        return auditRecord;
    }

    public AuditRecord securityRecord(String action,
                                      String objectType,
                                      String objectId,
                                      Long actorAccountId,
                                      String actorRole,
                                      AuditOutcome outcome,
                                      String requestId,
                                      String ipAddress,
                                      String userAgent,
                                      String details,
                                      Instant occurredAt) {
        AuditRecord auditRecord = baseRecord(
                null,
                "SecurityAuditEvent",
                "SECURITY",
                normalizeAction(action),
                defaultString(objectType, "SECURITY"),
                defaultString(objectId, "UNKNOWN"),
                outcome,
                safeJson(details),
                occurredAt
        );
        auditRecord.setActorAccountId(actorAccountId);
        auditRecord.setActorRole(actorRole);
        auditRecord.setRequestId(requestId);
        auditRecord.setIpAddress(ipAddress);
        auditRecord.setUserAgent(userAgent);
        return auditRecord;
    }

    public String actionFromEventType(String eventType) {
        String withoutSuffix = eventType == null ? "UNKNOWN" : eventType.replaceFirst("Event$", "");
        return normalizeAction(withoutSuffix);
    }

    public String normalizeSourceModule(String sourceModule) {
        return defaultString(sourceModule, "UNKNOWN").toUpperCase(Locale.ROOT);
    }

    public String normalizeActionValue(String action) {
        return normalizeAction(action);
    }

    private AuditRecord baseRecord(String eventId,
                                   String eventType,
                                   String sourceModule,
                                   String action,
                                   String objectType,
                                   String objectId,
                                   AuditOutcome outcome,
                                   String details,
                                   Instant occurredAt) {
        Instant now = Instant.now();
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setEventId(eventId);
        auditRecord.setEventType(defaultString(eventType, "UnknownAuditEvent"));
        auditRecord.setSourceModule(normalizeSourceModule(sourceModule));
        auditRecord.setAction(normalizeAction(action));
        auditRecord.setObjectType(defaultString(objectType, "UNKNOWN"));
        auditRecord.setObjectId(defaultString(objectId, "UNKNOWN"));
        auditRecord.setOutcome(outcome.name());
        auditRecord.setDetails(defaultString(details, "{}"));
        auditRecord.setOccurredAt(occurredAt == null ? now : occurredAt);
        auditRecord.setRecordedAt(now);
        return auditRecord;
    }

    private String normalizeAction(String value) {
        String safeValue = defaultString(value, "UNKNOWN");
        String withUnderscores = safeValue
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return withUnderscores.isBlank() ? "UNKNOWN" : withUnderscores.toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        try {
            return sensitiveDataMasker.mask(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String safeJson(String details) {
        if (details == null || details.isBlank()) {
            return "{}";
        }
        return sensitiveDataMasker.mask(details);
    }
}
