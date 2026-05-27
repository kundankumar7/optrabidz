package com.project.optrabidz.notification.infrastructure.entity;

import com.project.optrabidz.notification.domain.model.ReadStatus;
import com.project.optrabidz.notification.domain.model.RecipientDeliveryStatus;
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
        name = "notification_recipient",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_recipient_account", columnNames = {"notification_id", "account_id"})
)
public class NotificationRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipient_id", nullable = false, updatable = false)
    private Long recipientId;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "recipient_type", nullable = false, columnDefinition = "text")
    private String recipientType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "recipient_delivery_status", nullable = false, columnDefinition = "recipient_delivery_status_enum")
    private RecipientDeliveryStatus recipientDeliveryStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "read_status", nullable = false, columnDefinition = "read_status_enum")
    private ReadStatus readStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
