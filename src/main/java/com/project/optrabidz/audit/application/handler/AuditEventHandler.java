package com.project.optrabidz.audit.application.handler;

import com.project.optrabidz.audit.application.AuditService;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.common.outbox.OutboxEventProcessor;
import org.springframework.stereotype.Component;

@Component
public class AuditEventHandler implements OutboxEventProcessor {
    private final AuditService auditService;

    public AuditEventHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return true;
    }

    @Override
    public void process(OutboxEvent event) {
        auditService.recordOutboxSuccess(event);
    }
}
