package com.project.optrabidz.common.outbox;

import com.project.optrabidz.common.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "event_outbox")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_event_id", nullable = false, updatable = false)
    private Long outboxEventId;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false, columnDefinition = "text")
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, columnDefinition = "text")
    private String eventType;

    @Column(name = "source_module", nullable = false, updatable = false, columnDefinition = "text")
    private String sourceModule;

    @Column(name = "aggregate_type", updatable = false, columnDefinition = "text")
    private String aggregateType;

    @Column(name = "aggregate_id", updatable = false, columnDefinition = "text")
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    private OutboxEventStatus eventStatus;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", columnDefinition = "text")
    private String lockedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxEvent() {
    }

    public static OutboxEvent from(DomainEvent event,
                                   String eventId,
                                   String sourceModule,
                                   String aggregateType,
                                   String aggregateId,
                                   String payload,
                                   Instant now) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.eventId = eventId;
        outboxEvent.eventType = event.getClass().getSimpleName();
        outboxEvent.sourceModule = sourceModule;
        outboxEvent.aggregateType = aggregateType;
        outboxEvent.aggregateId = aggregateId;
        outboxEvent.payload = payload;
        outboxEvent.eventStatus = OutboxEventStatus.PENDING;
        outboxEvent.occurredAt = event.occurredAt();
        outboxEvent.availableAt = now;
        outboxEvent.retryCount = 0;
        outboxEvent.createdAt = now;
        outboxEvent.updatedAt = now;
        return outboxEvent;
    }

    public void markProcessing(String workerId, Instant now) {
        this.lockedBy = workerId;
        this.lockedAt = now;
        this.updatedAt = now;
    }

    public void markProcessed(Instant now) {
        this.eventStatus = OutboxEventStatus.PROCESSED;
        this.processedAt = now;
        this.lockedBy = null;
        this.lockedAt = null;
        this.updatedAt = now;
        this.lastError = null;
    }

    public void markFailed(String errorMessage, Instant nextAttemptAt, Instant now) {
        this.eventStatus = OutboxEventStatus.PENDING;
        this.retryCount++;
        this.availableAt = nextAttemptAt;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = errorMessage == null ? null : errorMessage.substring(0, Math.min(errorMessage.length(), 4000));
        this.updatedAt = now;
    }

    public Long getOutboxEventId() {
        return outboxEventId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getEventStatus() {
        return eventStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
