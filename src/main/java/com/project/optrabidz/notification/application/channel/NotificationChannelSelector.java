package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationChannelSelector {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NotificationChannelSelector(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ResolvedNotificationChannel> resolve(Long accountId, List<ChannelType> requestedChannels) {
        List<ResolvedNotificationChannel> resolved = new ArrayList<>();
        if (requestedChannels.contains(ChannelType.IN_APP)) {
            resolved.add(new ResolvedNotificationChannel(ChannelType.IN_APP));
        }
        if (requestedChannels.contains(ChannelType.EMAIL) && hasActiveSubscription(accountId, ChannelType.EMAIL)) {
            resolved.add(new ResolvedNotificationChannel(ChannelType.EMAIL));
        }
        if (requestedChannels.contains(ChannelType.PUSH) && hasActiveSubscription(accountId, ChannelType.PUSH)) {
            resolved.add(new ResolvedNotificationChannel(ChannelType.PUSH));
        }
        return resolved;
    }

    private boolean hasActiveSubscription(Long accountId, ChannelType channelType) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*)
                from notification_subscription
                where account_id = :accountId
                  and channel_type = cast(:channelType as channel_type_enum)
                  and subscription_state = 'ACTIVE'
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("channelType", channelType.name()), Long.class);
        return count != null && count > 0;
    }
}
