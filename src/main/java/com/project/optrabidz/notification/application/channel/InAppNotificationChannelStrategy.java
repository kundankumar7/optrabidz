package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannelStrategy implements NotificationChannelStrategy {
    @Override
    public ChannelType channelType() {
        return ChannelType.IN_APP;
    }

    @Override
    public NotificationSendResult send(NotificationDispatchContext context) {
        return NotificationSendResult.delivered("in-app-" + context.deliveryId());
    }
}
