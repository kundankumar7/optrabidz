package com.project.optrabidz.audit.application.dto.response;

import java.time.Instant;

public record AuditRecordResponse(
        Long auditRecordId,
        String eventId,
        String eventType,
        String sourceModule,
        String action,
        String objectType,
        String objectId,
        Long actorAccountId,
        String actorRole,
        String outcome,
        String requestId,
        String ipAddress,
        String userAgent,
        String details,
        Instant occurredAt,
        Instant recordedAt
) {
}
