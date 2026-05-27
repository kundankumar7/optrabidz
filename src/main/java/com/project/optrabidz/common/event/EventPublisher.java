package com.project.optrabidz.common.event;

public interface EventPublisher {
    void publish(DomainEvent event);
}
