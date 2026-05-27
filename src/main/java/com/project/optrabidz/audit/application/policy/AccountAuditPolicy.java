package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class AccountAuditPolicy implements AuditPolicy {
    private final ObjectMapper objectMapper;

    public AccountAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "AccountRegisteredEvent".equals(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long accountId = JsonAuditPayload.longValue(payload, "accountId");
        String roleType = JsonAuditPayload.textValue(payload, "roleType");
        return AuditDescriptor.success(
                "IDENTITY",
                "ACCOUNT_REGISTERED",
                "ACCOUNT",
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
