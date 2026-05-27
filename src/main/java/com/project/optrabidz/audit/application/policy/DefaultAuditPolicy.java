package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultAuditPolicy {
    public AuditDescriptor describe(OutboxEvent event) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("eventType", event.getEventType());
        details.put("aggregateType", event.getAggregateType());
        details.put("aggregateId", event.getAggregateId());

        return AuditDescriptor.success(
                event.getSourceModule(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                null,
                null,
                details
        );
    }
}
