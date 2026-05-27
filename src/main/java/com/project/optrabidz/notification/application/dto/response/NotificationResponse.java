package com.project.optrabidz.notification.application.dto.response;

import java.time.Instant;

public record NotificationResponse(
        Long recipientId,
        Long notificationId,
        String eventType,
        String notificationName,
        String notificationType,
        String entityType,
        Long entityId,
        String title,
        String body,
        String payload,
        String readStatus,
        String recipientDeliveryStatus,
        Instant occurredAt,
        Instant deliveredAt,
        Instant readAt
) {
}
