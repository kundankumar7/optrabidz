package com.project.optrabidz.notification.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.notification.application.channel.NotificationChannelSelector;
import com.project.optrabidz.notification.application.channel.NotificationDeliveryDispatcher;
import com.project.optrabidz.notification.application.channel.ResolvedNotificationChannel;
import com.project.optrabidz.notification.application.dto.request.CreateNotificationSubscriptionRequest;
import com.project.optrabidz.notification.application.dto.response.NotificationResponse;
import com.project.optrabidz.notification.application.dto.response.NotificationSubscriptionResponse;
import com.project.optrabidz.notification.application.exception.NotificationNotFoundException;
import com.project.optrabidz.notification.application.rule.NotificationPlan;
import com.project.optrabidz.notification.domain.model.ChannelDeliveryStatus;
import com.project.optrabidz.notification.domain.model.ChannelType;
import com.project.optrabidz.notification.domain.model.NotificationSubscriptionState;
import com.project.optrabidz.notification.domain.model.ReadStatus;
import com.project.optrabidz.notification.domain.model.RecipientDeliveryStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationChannelSelector channelSelector;
    private final NotificationDeliveryDispatcher deliveryDispatcher;

    public NotificationService(NamedParameterJdbcTemplate jdbcTemplate,
                               NotificationChannelSelector channelSelector,
                               NotificationDeliveryDispatcher deliveryDispatcher) {
        this.jdbcTemplate = jdbcTemplate;
        this.channelSelector = channelSelector;
        this.deliveryDispatcher = deliveryDispatcher;
    }

    @Transactional
    public void createFromPlan(OutboxEvent event, NotificationPlan plan) {
        if (plan.recipientAccountIds().isEmpty()) {
            return;
        }

        Long notificationId = insertOrFindNotification(event, plan);
        for (Long recipientAccountId : plan.recipientAccountIds()) {
            Long recipientId = insertOrFindRecipient(notificationId, recipientAccountId, plan);
            for (ResolvedNotificationChannel channel : channelSelector.resolve(recipientAccountId, plan.channels())) {
                Long deliveryId = insertOrFindDelivery(recipientId, channel.channelType());
                if (channel.channelType() == ChannelType.IN_APP) {
                    deliveryDispatcher.dispatchDeliveryNow(deliveryId);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyFeed(Long accountId, ReadStatus readStatus, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("limit", safeSize)
                .addValue("offset", offset);
        String readStatusFilter = "";
        if (readStatus != null) {
            params.addValue("readStatus", readStatus.name());
            readStatusFilter = " and r.read_status = cast(:readStatus as read_status_enum) ";
        }

        List<NotificationResponse> items = jdbcTemplate.query("""
                select
                    r.recipient_id,
                    n.notification_id,
                    n.event_type,
                    n.notification_name,
                    n.notification_type,
                    n.entity_type,
                    n.entity_id,
                    n.title,
                    n.body,
                    n.payload::text as payload,
                    r.read_status::text as read_status,
                    r.recipient_delivery_status::text as recipient_delivery_status,
                    r.occurred_at,
                    r.delivered_at,
                    r.read_at
                from notification_recipient r
                join notification n on n.notification_id = r.notification_id
                where r.account_id = :accountId
                  and r.is_deleted = false
                """ + readStatusFilter + """
                order by r.occurred_at desc, r.recipient_id desc
                limit :limit offset :offset
                """, params, new NotificationResponseRowMapper());

        long totalItems = jdbcTemplate.queryForObject("""
                select count(*)
                from notification_recipient r
                where r.account_id = :accountId
                  and r.is_deleted = false
                """ + readStatusFilter + """
                """, params, Long.class);

        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) safeSize);
        return new PageResponse<>(items, safePage, safeSize, totalItems, totalPages);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long accountId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from notification_recipient
                where account_id = :accountId
                  and is_deleted = false
                  and read_status = 'UNREAD'::read_status_enum
                """, new MapSqlParameterSource("accountId", accountId), Long.class);
    }

    @Transactional
    public void markRead(Long accountId, Long recipientId) {
        int updated = jdbcTemplate.update("""
                update notification_recipient
                set read_status = 'READ'::read_status_enum,
                    read_at = coalesce(read_at, :now)
                where recipient_id = :recipientId
                  and account_id = :accountId
                  and is_deleted = false
                """, new MapSqlParameterSource()
                .addValue("recipientId", recipientId)
                .addValue("accountId", accountId)
                .addValue("now", Timestamp.from(Instant.now())));

        if (updated == 0) {
            throw new NotificationNotFoundException();
        }
    }

    @Transactional
    public int markAllRead(Long accountId) {
        return jdbcTemplate.update("""
                update notification_recipient
                set read_status = 'READ'::read_status_enum,
                    read_at = coalesce(read_at, :now)
                where account_id = :accountId
                  and is_deleted = false
                  and read_status = 'UNREAD'::read_status_enum
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("now", Timestamp.from(Instant.now())));
    }

    @Transactional
    public void delete(Long accountId, Long recipientId) {
        int updated = jdbcTemplate.update("""
                update notification_recipient
                set is_deleted = true,
                    deleted_at = :now
                where recipient_id = :recipientId
                  and account_id = :accountId
                  and is_deleted = false
                """, new MapSqlParameterSource()
                .addValue("recipientId", recipientId)
                .addValue("accountId", accountId)
                .addValue("now", Timestamp.from(Instant.now())));

        if (updated == 0) {
            throw new NotificationNotFoundException();
        }
    }

    @Transactional
    public NotificationSubscriptionResponse saveSubscription(Long accountId, CreateNotificationSubscriptionRequest request) {
        Instant now = Instant.now();
        Long subscriptionId = jdbcTemplate.queryForObject("""
                insert into notification_subscription (
                    account_id,
                    channel_type,
                    endpoint,
                    public_key,
                    auth_secret,
                    subscription_state,
                    created_at,
                    updated_at
                )
                values (
                    :accountId,
                    cast(:channelType as channel_type_enum),
                    :endpoint,
                    :publicKey,
                    :authSecret,
                    :state,
                    :now,
                    :now
                )
                on conflict (account_id, channel_type, endpoint)
                do update set
                    public_key = excluded.public_key,
                    auth_secret = excluded.auth_secret,
                    subscription_state = excluded.subscription_state,
                    updated_at = excluded.updated_at,
                    revoked_at = null
                returning subscription_id
                """, new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("channelType", request.channelType().name())
                .addValue("endpoint", request.endpoint())
                .addValue("publicKey", request.publicKey())
                .addValue("authSecret", request.authSecret())
                .addValue("state", NotificationSubscriptionState.ACTIVE.name())
                .addValue("now", Timestamp.from(now)), Long.class);
        return new NotificationSubscriptionResponse(subscriptionId, "Notification subscription saved");
    }

    @Transactional
    public void revokeSubscription(Long accountId, Long subscriptionId) {
        int updated = jdbcTemplate.update("""
                update notification_subscription
                set subscription_state = :state,
                    revoked_at = :now,
                    updated_at = :now
                where subscription_id = :subscriptionId
                  and account_id = :accountId
                  and subscription_state = 'ACTIVE'
                """, new MapSqlParameterSource()
                .addValue("subscriptionId", subscriptionId)
                .addValue("accountId", accountId)
                .addValue("state", NotificationSubscriptionState.REVOKED.name())
                .addValue("now", Timestamp.from(Instant.now())));

        if (updated == 0) {
            throw new NotificationNotFoundException();
        }
    }

    private Long insertOrFindNotification(OutboxEvent event, NotificationPlan plan) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("eventId", event.getEventId())
                .addValue("eventType", event.getEventType())
                .addValue("notificationName", plan.notificationName())
                .addValue("notificationType", plan.notificationType())
                .addValue("entityType", plan.entityType())
                .addValue("entityId", plan.entityId())
                .addValue("title", plan.title())
                .addValue("body", plan.body())
                .addValue("payload", plan.payload())
                .addValue("occurredAt", Timestamp.from(plan.occurredAt()))
                .addValue("createdAt", Timestamp.from(Instant.now()));

        List<Long> inserted = jdbcTemplate.query("""
                insert into notification (
                    event_id,
                    event_type,
                    notification_name,
                    notification_type,
                    entity_type,
                    entity_id,
                    title,
                    body,
                    payload,
                    occurred_at,
                    created_at
                )
                values (
                    :eventId,
                    :eventType,
                    :notificationName,
                    :notificationType,
                    :entityType,
                    :entityId,
                    :title,
                    :body,
                    cast(:payload as jsonb),
                    :occurredAt,
                    :createdAt
                )
                on conflict (event_id, notification_name) do nothing
                returning notification_id
                """, params, (rs, rowNum) -> rs.getLong("notification_id"));

        if (!inserted.isEmpty()) {
            return inserted.getFirst();
        }

        return jdbcTemplate.queryForObject("""
                select notification_id
                from notification
                where event_id = :eventId
                  and notification_name = :notificationName
                """, params, Long.class);
    }

    private Long insertOrFindRecipient(Long notificationId, Long accountId, NotificationPlan plan) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("accountId", accountId)
                .addValue("recipientType", "ACCOUNT")
                .addValue("recipientDeliveryStatus", RecipientDeliveryStatus.DELIVERED.name())
                .addValue("readStatus", ReadStatus.UNREAD.name())
                .addValue("occurredAt", Timestamp.from(plan.occurredAt()))
                .addValue("deliveredAt", Timestamp.from(Instant.now()));

        List<Long> inserted = jdbcTemplate.query("""
                insert into notification_recipient (
                    notification_id,
                    account_id,
                    recipient_type,
                    recipient_delivery_status,
                    read_status,
                    occurred_at,
                    delivered_at,
                    is_deleted
                )
                values (
                    :notificationId,
                    :accountId,
                    :recipientType,
                    cast(:recipientDeliveryStatus as recipient_delivery_status_enum),
                    cast(:readStatus as read_status_enum),
                    :occurredAt,
                    :deliveredAt,
                    false
                )
                on conflict (notification_id, account_id) do nothing
                returning recipient_id
                """, params, (rs, rowNum) -> rs.getLong("recipient_id"));

        if (!inserted.isEmpty()) {
            return inserted.getFirst();
        }

        return jdbcTemplate.queryForObject("""
                select recipient_id
                from notification_recipient
                where notification_id = :notificationId
                  and account_id = :accountId
                """, params, Long.class);
    }

    private Long insertOrFindDelivery(Long recipientId, ChannelType channelType) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recipientId", recipientId)
                .addValue("channelType", channelType.name())
                .addValue("status", ChannelDeliveryStatus.PENDING.name())
                .addValue("attemptCount", 0);

        List<Long> inserted = jdbcTemplate.query("""
                insert into notification_delivery (
                    recipient_id,
                    channel_type,
                    channel_delivery_status,
                    attempt_count
                )
                values (
                    :recipientId,
                    cast(:channelType as channel_type_enum),
                    cast(:status as channel_delivery_status_enum),
                    :attemptCount
                )
                on conflict (recipient_id, channel_type) do nothing
                returning delivery_id
                """, params, (rs, rowNum) -> rs.getLong("delivery_id"));

        if (!inserted.isEmpty()) {
            return inserted.getFirst();
        }

        return jdbcTemplate.queryForObject("""
                select delivery_id
                from notification_delivery
                where recipient_id = :recipientId
                  and channel_type = cast(:channelType as channel_type_enum)
                """, params, Long.class);
    }

    private static class NotificationResponseRowMapper implements RowMapper<NotificationResponse> {
        @Override
        public NotificationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new NotificationResponse(
                    rs.getLong("recipient_id"),
                    rs.getLong("notification_id"),
                    rs.getString("event_type"),
                    rs.getString("notification_name"),
                    rs.getString("notification_type"),
                    rs.getString("entity_type"),
                    rs.getLong("entity_id"),
                    rs.getString("title"),
                    rs.getString("body"),
                    rs.getString("payload"),
                    rs.getString("read_status"),
                    rs.getString("recipient_delivery_status"),
                    toInstant(rs.getTimestamp("occurred_at")),
                    toInstant(rs.getTimestamp("delivered_at")),
                    toInstant(rs.getTimestamp("read_at"))
            );
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
