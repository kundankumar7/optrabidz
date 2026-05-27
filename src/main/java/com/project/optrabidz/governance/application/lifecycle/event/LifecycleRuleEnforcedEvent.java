package com.project.optrabidz.governance.application.lifecycle.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;
import java.util.List;

public record LifecycleRuleEnforcedEvent(
        String ruleName,
        int evaluatedCount,
        int changedCount,
        int failedCount,
        List<String> messages,
        Instant occurredAt
) implements DomainEvent {
    public LifecycleRuleEnforcedEvent {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
