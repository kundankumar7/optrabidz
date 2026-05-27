package com.project.optrabidz.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxWriter {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventMetadataResolver metadataResolver;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository outboxEventRepository,
                        OutboxEventMetadataResolver metadataResolver,
                        ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.metadataResolver = metadataResolver;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent write(DomainEvent event) {
        Instant now = Instant.now();
        OutboxEventMetadata metadata = metadataResolver.resolve(event);
        OutboxEvent outboxEvent = OutboxEvent.from(
                event,
                UUID.randomUUID().toString(),
                metadata.sourceModule(),
                metadata.aggregateType(),
                metadata.aggregateId(),
                serialize(event),
                now
        );
        return outboxEventRepository.save(outboxEvent);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Domain event could not be serialized for outbox", exception);
        }
    }
}
