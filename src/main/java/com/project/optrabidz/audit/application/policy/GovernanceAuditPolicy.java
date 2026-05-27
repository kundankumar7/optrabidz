package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class GovernanceAuditPolicy implements AuditPolicy {
    private static final String ADMIN_AUTHORITY_TRANSFERRED_EVENT = "AdminAuthorityTransferredEvent";
    private static final String LIFECYCLE_RULE_ENFORCED_EVENT = "LifecycleRuleEnforcedEvent";

    private final ObjectMapper objectMapper;

    public GovernanceAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return ADMIN_AUTHORITY_TRANSFERRED_EVENT.equals(event.getEventType())
                || LIFECYCLE_RULE_ENFORCED_EVENT.equals(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        if (LIFECYCLE_RULE_ENFORCED_EVENT.equals(event.getEventType())) {
            return describeLifecycleRule(event);
        }
        return describeAdminAuthorityTransfer(event);
    }

    private AuditDescriptor describeAdminAuthorityTransfer(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long revokedByAccountId = JsonAuditPayload.longValue(payload, "revokedByAccountId");
        Long newAdminAccountId = JsonAuditPayload.longValue(payload, "newAdminAccountId");
        return AuditDescriptor.success(
                "GOVERNANCE",
                "ADMIN_AUTHORITY_TRANSFERRED",
                "ADMIN_AUTHORITY",
                newAdminAccountId,
                revokedByAccountId,
                revokedByAccountId == null ? "SYSTEM" : "ADMIN",
                JsonAuditPayload.details(
                        "newAdminAccountId", newAdminAccountId,
                        "revokedAdminAccountId", JsonAuditPayload.longValue(payload, "revokedAdminAccountId"),
                        "revokedByAccountId", revokedByAccountId,
                        "revocationReason", JsonAuditPayload.textValue(payload, "revocationReason")
                )
        );
    }

    private AuditDescriptor describeLifecycleRule(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        String ruleName = JsonAuditPayload.textValue(payload, "ruleName");
        int failedCount = payload.path("failedCount").asInt(0);
        int changedCount = payload.path("changedCount").asInt(0);
        int evaluatedCount = payload.path("evaluatedCount").asInt(0);
        return new AuditDescriptor(
                "GOVERNANCE",
                failedCount > 0 ? "LIFECYCLE_RULE_FAILED" : "LIFECYCLE_RULE_ENFORCED",
                "LIFECYCLE_RULE",
                ruleName,
                null,
                "SYSTEM",
                failedCount > 0 ? AuditOutcome.FAILED : AuditOutcome.SYSTEM,
                JsonAuditPayload.details(
                        "ruleName", ruleName,
                        "evaluatedCount", evaluatedCount,
                        "changedCount", changedCount,
                        "failedCount", failedCount,
                        "messages", payload.path("messages")
                )
        );
    }
}
