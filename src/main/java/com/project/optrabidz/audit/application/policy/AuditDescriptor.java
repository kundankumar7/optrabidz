package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.audit.domain.model.AuditOutcome;

import java.util.Map;

public record AuditDescriptor(
        String sourceModule,
        String action,
        String objectType,
        String objectId,
        Long actorAccountId,
        String actorRole,
        AuditOutcome outcome,
        Map<String, Object> details
) {
    public static AuditDescriptor success(String sourceModule,
                                          String action,
                                          String objectType,
                                          Object objectId,
                                          Long actorAccountId,
                                          String actorRole,
                                          Map<String, Object> details) {
        return new AuditDescriptor(
                sourceModule,
                action,
                objectType,
                stringValue(objectId),
                actorAccountId,
                actorRole,
                AuditOutcome.SUCCESS,
                details
        );
    }

    public static AuditDescriptor failed(String sourceModule,
                                         String action,
                                         String objectType,
                                         Object objectId,
                                         Long actorAccountId,
                                         String actorRole,
                                         Map<String, Object> details) {
        return new AuditDescriptor(
                sourceModule,
                action,
                objectType,
                stringValue(objectId),
                actorAccountId,
                actorRole,
                AuditOutcome.FAILED,
                details
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
