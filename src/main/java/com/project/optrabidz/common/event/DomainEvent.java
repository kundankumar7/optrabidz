package com.project.optrabidz.common.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
