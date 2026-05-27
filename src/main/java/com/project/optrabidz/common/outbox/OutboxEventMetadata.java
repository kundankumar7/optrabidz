package com.project.optrabidz.common.outbox;

public record OutboxEventMetadata(
        String sourceModule,
        String aggregateType,
        String aggregateId
) {
}
