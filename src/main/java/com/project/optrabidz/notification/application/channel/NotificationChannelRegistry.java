package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationChannelRegistry {
    private final Map<ChannelType, NotificationChannelStrategy> strategies;

    public NotificationChannelRegistry(List<NotificationChannelStrategy> strategies) {
        this.strategies = new EnumMap<>(ChannelType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.channelType(), strategy));
    }

    public NotificationChannelStrategy get(ChannelType channelType) {
        NotificationChannelStrategy strategy = strategies.get(channelType);
        if (strategy == null) {
            throw new IllegalStateException("No notification channel strategy configured for " + channelType);
        }
        return strategy;
    }
}
