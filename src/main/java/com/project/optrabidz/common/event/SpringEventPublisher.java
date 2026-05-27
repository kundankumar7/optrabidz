package com.project.optrabidz.common.event;

import com.project.optrabidz.common.outbox.OutboxWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisher implements EventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxWriter outboxWriter;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                OutboxWriter outboxWriter) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void publish(DomainEvent event) {
        outboxWriter.write(event);
        applicationEventPublisher.publishEvent(event);
    }
}
