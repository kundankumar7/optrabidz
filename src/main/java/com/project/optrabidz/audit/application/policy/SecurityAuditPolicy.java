package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityAuditPolicy implements AuditPolicy {
    private static final String CREDENTIAL_PASSWORD_CHANGED_EVENT =
            "CredentialPasswordChangedEvent";

    private final ObjectMapper objectMapper;

    public SecurityAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return CREDENTIAL_PASSWORD_CHANGED_EVENT.equals(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long accountId = JsonAuditPayload.longValue(payload, "accountId");
        String actorRole = JsonAuditPayload.textValue(payload, "actorRole");
        int terminatedSessionCount = payload.path("terminatedSessionCount").asInt(0);

        return AuditDescriptor.success(
                "SECURITY",
                "PASSWORD_CHANGED",
                "CREDENTIAL",
                accountId,
                accountId,
                actorRole,
                JsonAuditPayload.details(
                        "accountId", accountId,
                        "actorRole", actorRole,
                        "terminatedSessionCount", terminatedSessionCount
                )
        );
    }
}
