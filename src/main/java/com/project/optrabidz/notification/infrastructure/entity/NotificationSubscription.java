package com.project.optrabidz.notification.infrastructure.entity;

import com.project.optrabidz.notification.domain.model.ChannelType;
import com.project.optrabidz.notification.domain.model.NotificationSubscriptionState;
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
        name = "notification_subscription",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_subscription_endpoint", columnNames = {"account_id", "channel_type", "endpoint"})
)
public class NotificationSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id", nullable = false, updatable = false)
    private Long subscriptionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel_type", nullable = false, columnDefinition = "channel_type_enum")
    private ChannelType channelType;

    @Column(name = "endpoint", nullable = false, columnDefinition = "text")
    private String endpoint;

    @Column(name = "public_key", columnDefinition = "text")
    private String publicKey;

    @Column(name = "auth_secret", columnDefinition = "text")
    private String authSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_state", nullable = false)
    private NotificationSubscriptionState subscriptionState;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
