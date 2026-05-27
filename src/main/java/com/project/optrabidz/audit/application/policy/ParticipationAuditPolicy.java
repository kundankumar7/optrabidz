package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class ParticipationAuditPolicy implements AuditPolicy {
    private final ObjectMapper objectMapper;

    public ParticipationAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "ParticipationProfileChangedEvent".equals(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long accountId = JsonAuditPayload.longValue(payload, "accountId");
        String roleType = JsonAuditPayload.textValue(payload, "roleType");
        return AuditDescriptor.success(
                "PARTICIPATION",
                "PARTICIPATION_PROFILE_CHANGED",
                "PARTICIPATION_PROFILE",
                accountId,
                accountId,
                roleType,
                JsonAuditPayload.details(
                        "accountId", accountId,
                        "roleType", roleType
                )
        );
    }
}
