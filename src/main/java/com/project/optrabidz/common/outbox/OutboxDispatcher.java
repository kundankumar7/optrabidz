package com.project.optrabidz.common.outbox;

import com.project.optrabidz.common.observability.OperationalEventLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "optrabidz.outbox.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventProcessor> processors;
    private final OperationalEventLogger operationalEventLogger;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final String workerId;

    public OutboxDispatcher(OutboxEventRepository outboxEventRepository,
                            List<OutboxEventProcessor> processors,
                            OperationalEventLogger operationalEventLogger,
                            TransactionTemplate transactionTemplate,
                            @Value("${optrabidz.outbox.dispatcher.batch-size:50}") int batchSize,
                            @Value("${optrabidz.outbox.dispatcher.worker-id:}") String configuredWorkerId) {
        this.outboxEventRepository = outboxEventRepository;
        this.processors = processors;
        this.operationalEventLogger = operationalEventLogger;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = Math.max(batchSize, 1);
        this.workerId = configuredWorkerId == null || configuredWorkerId.isBlank()
                ? "outbox-" + UUID.randomUUID()
                : configuredWorkerId;
    }

    @Scheduled(
            initialDelayString = "${optrabidz.outbox.dispatcher.initial-delay-ms:5000}",
            fixedDelayString = "${optrabidz.outbox.dispatcher.fixed-delay-ms:5000}"
    )
    public int dispatchPending() {
        int processed = 0;
        for (int i = 0; i < batchSize; i++) {
            DispatchFailure failure = new DispatchFailure();
            try {
                Boolean dispatched = transactionTemplate.execute(status -> dispatchOne(failure));
                if (!Boolean.TRUE.equals(dispatched)) {
                    break;
                }
                processed++;
            } catch (RuntimeException exception) {
                if (failure.outboxEventId != null) {
                    markFailed(failure, exception);
                    processed++;
                } else {
                    throw exception;
                }
            }
        }
        return processed;
    }

    private Boolean dispatchOne(DispatchFailure failure) {
        Instant now = Instant.now();
        List<Long> ids = outboxEventRepository.lockDispatchableIds(now, 1);
        if (ids.isEmpty()) {
            return false;
        }

        OutboxEvent event = outboxEventRepository.findById(ids.getFirst()).orElseThrow();
        failure.outboxEventId = event.getOutboxEventId();
        failure.eventId = event.getEventId();
        failure.eventType = event.getEventType();
        event.markProcessing(workerId, now);
        processors.stream()
                .filter(processor -> processor.supports(event))
                .forEach(processor -> processor.process(event));
        event.markProcessed(Instant.now());
        return true;
    }

    private void markFailed(DispatchFailure failure, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(failure.outboxEventId)
                .ifPresent(event -> {
                    Instant retryAt = Instant.now().plus(retryDelay(event.getRetryCount()));
                    event.markFailed(exception.getMessage(), retryAt, Instant.now());
                    operationalEventLogger.warn(
                            "OUTBOX_DISPATCH_RETRY",
                            "eventId=" + failure.eventId + " eventType=" + failure.eventType + " retryAt=" + retryAt
                    );
                    operationalEventLogger.error(
                            "OUTBOX_DISPATCH_FAILURE",
                            "eventId=" + failure.eventId + " eventType=" + failure.eventType,
                            exception
                    );
                }));
    }

    private Duration retryDelay(int retryCount) {
        long seconds = Math.min(300, (long) Math.pow(2, Math.min(retryCount, 8)));
        return Duration.ofSeconds(seconds);
    }

    private static class DispatchFailure {
        private Long outboxEventId;
        private String eventId;
        private String eventType;
    }
}
