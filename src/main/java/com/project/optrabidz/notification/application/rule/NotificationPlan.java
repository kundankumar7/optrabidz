package com.project.optrabidz.notification.application.rule;

import com.project.optrabidz.notification.domain.model.ChannelType;

import java.time.Instant;
import java.util.List;

public record NotificationPlan(
        String notificationName,
        String notificationType,
        String entityType,
        Long entityId,
        String title,
        String body,
        String payload,
        Instant occurredAt,
        List<Long> recipientAccountIds,
        List<ChannelType> channels
) {
    public NotificationPlan {
        recipientAccountIds = recipientAccountIds == null ? List.of() : recipientAccountIds.stream().distinct().toList();
        channels = channels == null || channels.isEmpty() ? List.of(ChannelType.IN_APP) : channels.stream().distinct().toList();
    }
}
