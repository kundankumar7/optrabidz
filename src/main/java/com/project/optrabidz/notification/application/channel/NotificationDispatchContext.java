package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;

public record NotificationDispatchContext(
        Long deliveryId,
        Long recipientId,
        Long notificationId,
        Long accountId,
        ChannelType channelType,
        String endpoint,
        String notificationName,
        String notificationType,
        String entityType,
        Long entityId,
        String title,
        String body,
        String payload,
        int nextAttemptNumber
) {
}
