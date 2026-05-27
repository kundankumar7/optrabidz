package com.project.optrabidz.notification.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "notification",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_idempotency", columnNames = {"event_id", "notification_name"})
)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false, updatable = false)
    private Long notificationId;

    @Column(name = "event_id", nullable = false, updatable = false, columnDefinition = "text")
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, columnDefinition = "text")
    private String eventType;

    @Column(name = "notification_name", nullable = false, updatable = false, columnDefinition = "text")
    private String notificationName;

    @Column(name = "notification_type", nullable = false, updatable = false, columnDefinition = "text")
    private String notificationType;

    @Column(name = "entity_type", nullable = false, updatable = false, columnDefinition = "text")
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "title", nullable = false, updatable = false, columnDefinition = "text")
    private String title;

    @Column(name = "body", nullable = false, updatable = false, columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
