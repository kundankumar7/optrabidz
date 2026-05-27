package com.project.optrabidz.common.outbox;

public interface OutboxEventProcessor {
    boolean supports(OutboxEvent event);

    void process(OutboxEvent event);
}
