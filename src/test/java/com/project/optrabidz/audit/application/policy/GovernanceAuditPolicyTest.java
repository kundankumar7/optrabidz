package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.governance.application.lifecycle.event.LifecycleRuleEnforcedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceAuditPolicyTest {
    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");

    private final GovernanceAuditPolicy policy = new GovernanceAuditPolicy(new ObjectMapper());

    @Test
    void mapsChangedLifecycleRuleToSystemAuditDescriptor() {
        AuditDescriptor descriptor = policy.describe(outboxEvent(
                new LifecycleRuleEnforcedEvent("listing-expiry", 2, 2, 0, List.of(), NOW),
                """
                        {
                          "ruleName": "listing-expiry",
                          "evaluatedCount": 2,
                          "changedCount": 2,
                          "failedCount": 0,
                          "messages": []
                        }
                        """
        ));

        assertThat(descriptor.action()).isEqualTo("LIFECYCLE_RULE_ENFORCED");
        assertThat(descriptor.objectType()).isEqualTo("LIFECYCLE_RULE");
        assertThat(descriptor.objectId()).isEqualTo("listing-expiry");
        assertThat(descriptor.actorRole()).isEqualTo("SYSTEM");
        assertThat(descriptor.outcome()).isEqualTo(AuditOutcome.SYSTEM);
    }

    @Test
    void mapsFailedLifecycleRuleToFailedAuditDescriptor() {
        AuditDescriptor descriptor = policy.describe(outboxEvent(
                new LifecycleRuleEnforcedEvent("settlement-expiry", 0, 0, 1, List.of("planned failure"), NOW),
                """
                        {
                          "ruleName": "settlement-expiry",
                          "evaluatedCount": 0,
                          "changedCount": 0,
                          "failedCount": 1,
                          "messages": ["planned failure"]
                        }
                        """
        ));

        assertThat(descriptor.action()).isEqualTo("LIFECYCLE_RULE_FAILED");
        assertThat(descriptor.objectId()).isEqualTo("settlement-expiry");
        assertThat(descriptor.actorRole()).isEqualTo("SYSTEM");
        assertThat(descriptor.outcome()).isEqualTo(AuditOutcome.FAILED);
    }

    private OutboxEvent outboxEvent(LifecycleRuleEnforcedEvent domainEvent, String payload) {
        return OutboxEvent.from(
                domainEvent,
                domainEvent.ruleName() + "-1",
                "GOVERNANCE",
                "LIFECYCLE_RULE",
                domainEvent.ruleName(),
                payload,
                NOW
        );
    }
}
