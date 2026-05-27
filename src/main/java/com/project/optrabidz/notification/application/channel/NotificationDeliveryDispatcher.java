package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.notification.domain.model.ChannelDeliveryStatus;
import com.project.optrabidz.notification.domain.model.ChannelType;
import com.project.optrabidz.notification.domain.model.NotificationDeliveryAttemptStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "optrabidz.notification.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationDeliveryDispatcher {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationChannelRegistry channelRegistry;
    private final NotificationChannelProxy channelProxy;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int maxAttempts;
    private final String workerId;

    public NotificationDeliveryDispatcher(NamedParameterJdbcTemplate jdbcTemplate,
                                          NotificationChannelRegistry channelRegistry,
                                          NotificationChannelProxy channelProxy,
                                          TransactionTemplate transactionTemplate,
                                          @Value("${optrabidz.notification.dispatcher.batch-size:50}") int batchSize,
                                          @Value("${optrabidz.notification.dispatcher.max-attempts:3}") int maxAttempts,
                                          @Value("${optrabidz.notification.dispatcher.worker-id:}") String configuredWorkerId) {
        this.jdbcTemplate = jdbcTemplate;
        this.channelRegistry = channelRegistry;
        this.channelProxy = channelProxy;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = Math.max(batchSize, 1);
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.workerId = configuredWorkerId == null || configuredWorkerId.isBlank()
                ? "notification-" + UUID.randomUUID()
                : configuredWorkerId;
    }

    @Scheduled(
            initialDelayString = "${optrabidz.notification.dispatcher.initial-delay-ms:5000}",
            fixedDelayString = "${optrabidz.notification.dispatcher.fixed-delay-ms:5000}"
    )
    public int dispatchReadyDeliveries() {
        int processed = 0;
        for (int index = 0; index < batchSize; index++) {
            Boolean dispatched = transactionTemplate.execute(status -> dispatchOneReadyDelivery());
            if (!Boolean.TRUE.equals(dispatched)) {
                break;
            }
            processed++;
        }
        return processed;
    }

    public boolean dispatchDeliveryNow(Long deliveryId) {
        Boolean dispatched = transactionTemplate.execute(status -> dispatchDelivery(deliveryId));
        return Boolean.TRUE.equals(dispatched);
    }

    private Boolean dispatchOneReadyDelivery() {
        List<Long> deliveryIds = lockReadyDeliveryIds(Instant.now(), 1);
        if (deliveryIds.isEmpty()) {
            return false;
        }
        return dispatchDelivery(deliveryIds.getFirst());
    }

    private Boolean dispatchDelivery(Long deliveryId) {
        NotificationDispatchContext context = loadDispatchContext(deliveryId);
        if (context == null) {
            return false;
        }

        Instant attemptStartedAt = Instant.now();
        markAttempting(context.deliveryId(), attemptStartedAt);

        NotificationChannelStrategy strategy = channelRegistry.get(context.channelType());
        long startNanos = System.nanoTime();
        NotificationSendResult result = channelProxy.send(strategy, context);
        long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        Instant completedAt = Instant.now();

        recordAttempt(context, result, completedAt, durationMs);
        if (result.successful()) {
            markDelivered(context, result, completedAt);
        } else {
            markFailed(context, result, completedAt);
        }
        refreshRecipientStatus(context.recipientId(), completedAt);
        return true;
    }

    private List<Long> lockReadyDeliveryIds(Instant now, int limit) {
        return jdbcTemplate.query("""
                select delivery_id
                from notification_delivery
                where (
                    channel_delivery_status = 'PENDING'::channel_delivery_status_enum
                    or (
                        channel_delivery_status = 'FAILED'::channel_delivery_status_enum
                        and next_attempt_at is not null
                        and next_attempt_at <= :now
                        and attempt_count < :maxAttempts
                    )
                )
                  and (next_attempt_at is null or next_attempt_at <= :now)
                order by coalesce(next_attempt_at, last_attempt_at, delivered_at), delivery_id
                for update skip locked
                limit :limit
                """, new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("maxAttempts", maxAttempts)
                .addValue("limit", limit), (rs, rowNum) -> rs.getLong("delivery_id"));
    }

    private NotificationDispatchContext loadDispatchContext(Long deliveryId) {
        List<NotificationDispatchContext> contexts = jdbcTemplate.query("""
                select
                    d.delivery_id,
                    d.recipient_id,
                    d.channel_type::text as channel_type,
                    d.attempt_count,
                    r.account_id,
                    n.notification_id,
                    n.notification_name,
                    n.notification_type,
                    n.entity_type,
                    n.entity_id,
                    n.title,
                    n.body,
                    n.payload::text as payload,
                    (
                        select ns.endpoint
                        from notification_subscription ns
                        where ns.account_id = r.account_id
                          and ns.channel_type = d.channel_type
                          and ns.subscription_state = 'ACTIVE'
                        order by ns.updated_at desc, ns.subscription_id desc
                        limit 1
                    ) as endpoint
                from notification_delivery d
                join notification_recipient r on r.recipient_id = d.recipient_id
                join notification n on n.notification_id = r.notification_id
                where d.delivery_id = :deliveryId
                  and (
                    d.channel_delivery_status = 'PENDING'::channel_delivery_status_enum
                    or (
                        d.channel_delivery_status = 'FAILED'::channel_delivery_status_enum
                        and d.attempt_count < :maxAttempts
                    )
                  )
                for update of d
                """, new MapSqlParameterSource()
                .addValue("deliveryId", deliveryId)
                .addValue("maxAttempts", maxAttempts), new NotificationDispatchContextRowMapper());
        return contexts.isEmpty() ? null : contexts.getFirst();
    }

    private void markAttempting(Long deliveryId, Instant now) {
        jdbcTemplate.update("""
                update notification_delivery
                set channel_delivery_status = 'ATTEMPTING'::channel_delivery_status_enum,
                    locked_at = :now,
                    locked_by = :workerId
                where delivery_id = :deliveryId
                """, new MapSqlParameterSource()
                .addValue("deliveryId", deliveryId)
                .addValue("now", Timestamp.from(now))
                .addValue("workerId", workerId));
    }

    private void recordAttempt(NotificationDispatchContext context,
                               NotificationSendResult result,
                               Instant attemptedAt,
                               long durationMs) {
        NotificationDeliveryAttemptStatus attemptStatus = result.successful()
                ? NotificationDeliveryAttemptStatus.DELIVERED
                : NotificationDeliveryAttemptStatus.FAILED;
        jdbcTemplate.update("""
                insert into notification_delivery_attempt (
                    delivery_id,
                    attempt_number,
                    attempt_status,
                    provider_message_id,
                    error_code,
                    error_message,
                    attempted_at,
                    duration_ms
                )
                values (
                    :deliveryId,
                    :attemptNumber,
                    :attemptStatus,
                    :providerMessageId,
                    :errorCode,
                    :errorMessage,
                    :attemptedAt,
                    :durationMs
                )
                on conflict (delivery_id, attempt_number) do nothing
                """, new MapSqlParameterSource()
                .addValue("deliveryId", context.deliveryId())
                .addValue("attemptNumber", context.nextAttemptNumber())
                .addValue("attemptStatus", attemptStatus.name())
                .addValue("providerMessageId", result.providerMessageId())
                .addValue("errorCode", result.errorCode())
                .addValue("errorMessage", result.errorMessage())
                .addValue("attemptedAt", Timestamp.from(attemptedAt))
                .addValue("durationMs", durationMs));
    }

    private void markDelivered(NotificationDispatchContext context,
                               NotificationSendResult result,
                               Instant now) {
        jdbcTemplate.update("""
                update notification_delivery
                set channel_delivery_status = 'DELIVERED'::channel_delivery_status_enum,
                    attempt_count = :attemptNumber,
                    last_attempt_at = :now,
                    delivered_at = :now,
                    failed_at = null,
                    provider_message_id = :providerMessageId,
                    failure_reason = null,
                    next_attempt_at = null,
                    locked_at = null,
                    locked_by = null
                where delivery_id = :deliveryId
                """, new MapSqlParameterSource()
                .addValue("deliveryId", context.deliveryId())
                .addValue("attemptNumber", context.nextAttemptNumber())
                .addValue("now", Timestamp.from(now))
                .addValue("providerMessageId", result.providerMessageId()));
    }

    private void markFailed(NotificationDispatchContext context,
                            NotificationSendResult result,
                            Instant now) {
        boolean shouldRetry = result.retryable() && context.nextAttemptNumber() < maxAttempts;
        Instant nextAttemptAt = shouldRetry ? now.plus(retryDelay(context.nextAttemptNumber())) : null;
        jdbcTemplate.update("""
                update notification_delivery
                set channel_delivery_status = 'FAILED'::channel_delivery_status_enum,
                    attempt_count = :attemptNumber,
                    last_attempt_at = :now,
                    failed_at = :now,
                    failure_reason = :failureReason,
                    next_attempt_at = :nextAttemptAt,
                    locked_at = null,
                    locked_by = null
                where delivery_id = :deliveryId
                """, new MapSqlParameterSource()
                .addValue("deliveryId", context.deliveryId())
                .addValue("attemptNumber", context.nextAttemptNumber())
                .addValue("now", Timestamp.from(now))
                .addValue("failureReason", result.errorCode() + ": " + result.errorMessage())
                .addValue("nextAttemptAt", nextAttemptAt == null ? null : Timestamp.from(nextAttemptAt)));
    }

    private void refreshRecipientStatus(Long recipientId, Instant now) {
        jdbcTemplate.update("""
                with stats as (
                    select
                        count(*) as total_count,
                        count(*) filter (
                            where channel_delivery_status = 'DELIVERED'::channel_delivery_status_enum
                        ) as delivered_count,
                        count(*) filter (
                            where channel_delivery_status = 'FAILED'::channel_delivery_status_enum
                              and (next_attempt_at is null or attempt_count >= :maxAttempts)
                        ) as final_failed_count
                    from notification_delivery
                    where recipient_id = :recipientId
                )
                update notification_recipient r
                set recipient_delivery_status = (
                    case
                        when stats.total_count = 0 then 'PENDING'
                        when stats.delivered_count = stats.total_count then 'DELIVERED'
                        when stats.delivered_count > 0 then 'PARTIALLY_DELIVERED'
                        when stats.final_failed_count = stats.total_count then 'FAILED'
                        else 'PENDING'
                    end
                )::recipient_delivery_status_enum,
                    delivered_at = case
                        when stats.delivered_count > 0 then coalesce(r.delivered_at, :now)
                        else r.delivered_at
                    end
                from stats
                where r.recipient_id = :recipientId
                """, new MapSqlParameterSource()
                .addValue("recipientId", recipientId)
                .addValue("maxAttempts", maxAttempts)
                .addValue("now", Timestamp.from(now)));
    }

    private Duration retryDelay(int attemptNumber) {
        long seconds = Math.min(300, (long) Math.pow(2, Math.max(attemptNumber - 1, 0)));
        return Duration.ofSeconds(seconds);
    }

    private static class NotificationDispatchContextRowMapper implements RowMapper<NotificationDispatchContext> {
        @Override
        public NotificationDispatchContext mapRow(ResultSet rs, int rowNum) throws SQLException {
            int nextAttemptNumber = rs.getInt("attempt_count") + 1;
            return new NotificationDispatchContext(
                    rs.getLong("delivery_id"),
                    rs.getLong("recipient_id"),
                    rs.getLong("notification_id"),
                    rs.getLong("account_id"),
                    ChannelType.valueOf(rs.getString("channel_type")),
                    rs.getString("endpoint"),
                    rs.getString("notification_name"),
                    rs.getString("notification_type"),
                    rs.getString("entity_type"),
                    rs.getLong("entity_id"),
                    rs.getString("title"),
                    rs.getString("body"),
                    rs.getString("payload"),
                    nextAttemptNumber
            );
        }
    }
}
