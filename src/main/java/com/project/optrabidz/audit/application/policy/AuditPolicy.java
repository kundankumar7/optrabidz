package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.common.outbox.OutboxEvent;

public interface AuditPolicy {
    boolean supports(OutboxEvent event);

    AuditDescriptor describe(OutboxEvent event);
}
