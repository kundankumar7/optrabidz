package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ClassificationAuditPolicy implements AuditPolicy {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "StartupClassificationChangedEvent",
            "InvestorPreferenceChangedEvent"
    );

    private final ObjectMapper objectMapper;

    public ClassificationAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return SUPPORTED_EVENTS.contains(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long accountId = JsonAuditPayload.longValue(payload, "accountId");

        if ("StartupClassificationChangedEvent".equals(event.getEventType())) {
            Long startupId = JsonAuditPayload.longValue(payload, "startupId");
            return AuditDescriptor.success(
                    "CLASSIFICATION",
                    "STARTUP_CLASSIFICATION_CHANGED",
                    "STARTUP_CLASSIFICATION",
                    startupId,
                    accountId,
                    "STARTUP",
                    JsonAuditPayload.details(
                            "startupId", startupId,
                            "accountId", accountId
                    )
            );
        }

        Long investorId = JsonAuditPayload.longValue(payload, "investorId");
        return AuditDescriptor.success(
                "CLASSIFICATION",
                "INVESTOR_PREFERENCE_CHANGED",
                "INVESTOR_PREFERENCE",
                investorId,
                accountId,
                "INVESTOR",
                JsonAuditPayload.details(
                        "investorId", investorId,
                        "accountId", accountId
                )
        );
    }
}
