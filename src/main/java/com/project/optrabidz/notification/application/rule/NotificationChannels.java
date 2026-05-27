package com.project.optrabidz.notification.application.rule;

import com.project.optrabidz.notification.domain.model.ChannelType;

import java.util.List;

public final class NotificationChannels {
    private NotificationChannels() {
    }

    public static List<ChannelType> standard() {
        return List.of(ChannelType.IN_APP, ChannelType.EMAIL, ChannelType.PUSH);
    }
}
