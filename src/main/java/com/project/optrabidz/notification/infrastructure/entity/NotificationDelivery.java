package com.project.optrabidz.notification.infrastructure.entity;

import com.project.optrabidz.notification.domain.model.ChannelDeliveryStatus;
import com.project.optrabidz.notification.domain.model.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "notification_delivery",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_delivery_channel", columnNames = {"recipient_id", "channel_type"})
)
public class NotificationDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id", nullable = false, updatable = false)
    private Long deliveryId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel_type", nullable = false, columnDefinition = "channel_type_enum")
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel_delivery_status", nullable = false, columnDefinition = "channel_delivery_status_enum")
    private ChannelDeliveryStatus channelDeliveryStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "provider_message_id", columnDefinition = "text")
    private String providerMessageId;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", columnDefinition = "text")
    private String lockedBy;
}
