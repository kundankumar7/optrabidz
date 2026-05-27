package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditPolicyRegistry {
    private final List<AuditPolicy> policies;
    private final DefaultAuditPolicy defaultAuditPolicy;

    public AuditPolicyRegistry(List<AuditPolicy> policies,
                               DefaultAuditPolicy defaultAuditPolicy) {
        this.policies = policies;
        this.defaultAuditPolicy = defaultAuditPolicy;
    }

    public AuditDescriptor describe(OutboxEvent event) {
        return policies.stream()
                .filter(policy -> policy.supports(event))
                .findFirst()
                .map(policy -> policy.describe(event))
                .orElseGet(() -> defaultAuditPolicy.describe(event));
    }
}
