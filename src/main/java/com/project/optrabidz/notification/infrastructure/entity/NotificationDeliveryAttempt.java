package com.project.optrabidz.notification.infrastructure.entity;

import com.project.optrabidz.notification.domain.model.NotificationDeliveryAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "notification_delivery_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_delivery_attempt_number", columnNames = {"delivery_id", "attempt_number"})
)
public class NotificationDeliveryAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_attempt_id", nullable = false, updatable = false)
    private Long deliveryAttemptId;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_status", nullable = false)
    private NotificationDeliveryAttemptStatus attemptStatus;

    @Column(name = "provider_message_id", columnDefinition = "text")
    private String providerMessageId;

    @Column(name = "error_code", columnDefinition = "text")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "duration_ms")
    private Long durationMs;
}
