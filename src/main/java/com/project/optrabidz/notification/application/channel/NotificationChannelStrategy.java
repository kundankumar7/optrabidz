package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;

public interface NotificationChannelStrategy {
    ChannelType channelType();

    NotificationSendResult send(NotificationDispatchContext context);
}
