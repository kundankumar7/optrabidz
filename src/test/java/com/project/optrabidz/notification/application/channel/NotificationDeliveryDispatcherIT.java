package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestPropertySource(properties = "optrabidz.notification.dispatcher.initial-delay-ms=86400000")
class NotificationDeliveryDispatcherIT extends PostgresIntegrationTestSupport {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationChannelProxy channelProxy;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void queuedUnsupportedChannelFailsTerminallyAndDoesNotBlockSupportedDelivery() {
        DeliveryFixture fixture = createPendingEmailAndInAppDeliveries("unsupported-channel-pending-probe");
        NotificationDeliveryDispatcher dispatcher = dispatcherWithOnlyInAppStrategy();

        assertThat(dispatcher.dispatchReadyDeliveries()).isEqualTo(2);

        Map<String, Object> emailDelivery = jdbcTemplate.getJdbcTemplate().queryForMap("""
                select
                    channel_delivery_status::text as status,
                    attempt_count,
                    failure_reason,
                    next_attempt_at,
                    locked_at,
                    locked_by
                from notification_delivery
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        assertThat(emailDelivery)
                .containsEntry("status", "FAILED")
                .containsEntry("attempt_count", 1)
                .containsEntry("failure_reason",
                        "CHANNEL_UNAVAILABLE: No notification channel strategy configured for EMAIL")
                .containsEntry("next_attempt_at", null)
                .containsEntry("locked_at", null)
                .containsEntry("locked_by", null);

        Map<String, Object> emailAttempt = jdbcTemplate.getJdbcTemplate().queryForMap("""
                select
                    attempt_status,
                    attempt_number,
                    error_code,
                    error_message
                from notification_delivery_attempt
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        assertThat(emailAttempt)
                .containsEntry("attempt_status", "FAILED")
                .containsEntry("attempt_number", 1)
                .containsEntry("error_code", "CHANNEL_UNAVAILABLE")
                .containsEntry("error_message", "No notification channel strategy configured for EMAIL");

        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select channel_delivery_status::text
                from notification_delivery
                where delivery_id = ?
                """, String.class, fixture.inAppDeliveryId())).isEqualTo("DELIVERED");
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select recipient_delivery_status::text
                from notification_recipient
                where recipient_id = ?
                """, String.class, fixture.recipientId())).isEqualTo("PARTIALLY_DELIVERED");

        assertThat(dispatcher.dispatchReadyDeliveries()).isZero();
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select count(*)
                from notification_delivery_attempt
                where delivery_id = ?
                """, Long.class, fixture.emailDeliveryId())).isEqualTo(1L);
    }

    @Test
    void retryableUnsupportedChannelFailsTerminallyOnItsNextAttempt() {
        DeliveryFixture fixture = createPendingEmailAndInAppDeliveries("unsupported-channel-retry-probe");
        jdbcTemplate.getJdbcTemplate().update("""
                update notification_delivery
                set channel_delivery_status = 'FAILED'::channel_delivery_status_enum,
                    attempt_count = 1,
                    last_attempt_at = now() - interval '2 minutes',
                    failed_at = now() - interval '2 minutes',
                    failure_reason = 'CHANNEL_EXCEPTION: temporary provider failure',
                    next_attempt_at = now() - interval '1 minute'
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        jdbcTemplate.getJdbcTemplate().update("""
                insert into notification_delivery_attempt (
                    delivery_id,
                    attempt_number,
                    attempt_status,
                    error_code,
                    error_message,
                    attempted_at,
                    duration_ms
                )
                values (?, 1, 'FAILED', 'CHANNEL_EXCEPTION', 'temporary provider failure',
                        now() - interval '2 minutes', 5)
                """, fixture.emailDeliveryId());
        NotificationDeliveryDispatcher dispatcher = dispatcherWithOnlyInAppStrategy();

        assertThat(dispatcher.dispatchReadyDeliveries()).isEqualTo(2);

        Map<String, Object> emailDelivery = jdbcTemplate.getJdbcTemplate().queryForMap("""
                select
                    channel_delivery_status::text as status,
                    attempt_count,
                    failure_reason,
                    next_attempt_at,
                    locked_at,
                    locked_by
                from notification_delivery
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        assertThat(emailDelivery)
                .containsEntry("status", "FAILED")
                .containsEntry("attempt_count", 2)
                .containsEntry("failure_reason",
                        "CHANNEL_UNAVAILABLE: No notification channel strategy configured for EMAIL")
                .containsEntry("next_attempt_at", null)
                .containsEntry("locked_at", null)
                .containsEntry("locked_by", null);

        Map<String, Object> attemptStats = jdbcTemplate.getJdbcTemplate().queryForMap("""
                select
                    count(*) as attempt_count,
                    max(attempt_number) as latest_attempt_number,
                    max(error_code) filter (where attempt_number = 2) as latest_error_code
                from notification_delivery_attempt
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        assertThat(attemptStats)
                .containsEntry("attempt_count", 2L)
                .containsEntry("latest_attempt_number", 2)
                .containsEntry("latest_error_code", "CHANNEL_UNAVAILABLE");

        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select channel_delivery_status::text
                from notification_delivery
                where delivery_id = ?
                """, String.class, fixture.inAppDeliveryId())).isEqualTo("DELIVERED");
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select recipient_delivery_status::text
                from notification_recipient
                where recipient_id = ?
                """, String.class, fixture.recipientId())).isEqualTo("PARTIALLY_DELIVERED");

        assertThat(dispatcher.dispatchReadyDeliveries()).isZero();
    }

    @Test
    void loweredAttemptLimitReconcilesExhaustedDeliveryWithoutAnotherAttempt() {
        DeliveryFixture fixture = createPendingEmailAndInAppDeliveries("lowered-attempt-limit-probe");
        jdbcTemplate.getJdbcTemplate().update("""
                update notification_delivery
                set channel_delivery_status = 'FAILED'::channel_delivery_status_enum,
                    attempt_count = 2,
                    last_attempt_at = now() - interval '2 minutes',
                    failed_at = now() - interval '2 minutes',
                    failure_reason = 'CHANNEL_EXCEPTION: second temporary provider failure',
                    next_attempt_at = now() - interval '1 minute',
                    locked_at = now() - interval '2 minutes',
                    locked_by = 'previous-worker'
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        insertFailedAttempt(fixture.emailDeliveryId(), 1, "first temporary provider failure");
        insertFailedAttempt(fixture.emailDeliveryId(), 2, "second temporary provider failure");
        NotificationDeliveryDispatcher dispatcher = dispatcherWithOnlyInAppStrategy(2);

        assertThat(dispatcher.dispatchReadyDeliveries()).isEqualTo(1);

        Map<String, Object> emailDelivery = jdbcTemplate.getJdbcTemplate().queryForMap("""
                select
                    channel_delivery_status::text as status,
                    attempt_count,
                    failure_reason,
                    next_attempt_at,
                    locked_at,
                    locked_by
                from notification_delivery
                where delivery_id = ?
                """, fixture.emailDeliveryId());
        assertThat(emailDelivery)
                .containsEntry("status", "FAILED")
                .containsEntry("attempt_count", 2)
                .containsEntry("failure_reason", "CHANNEL_EXCEPTION: second temporary provider failure")
                .containsEntry("next_attempt_at", null)
                .containsEntry("locked_at", null)
                .containsEntry("locked_by", null);
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select count(*)
                from notification_delivery_attempt
                where delivery_id = ?
                """, Long.class, fixture.emailDeliveryId())).isEqualTo(2L);
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select recipient_delivery_status::text
                from notification_recipient
                where recipient_id = ?
                """, String.class, fixture.recipientId())).isEqualTo("PARTIALLY_DELIVERED");
        assertThat(jdbcTemplate.getJdbcTemplate().queryForObject("""
                select channel_delivery_status::text
                from notification_delivery
                where delivery_id = ?
                """, String.class, fixture.inAppDeliveryId())).isEqualTo("DELIVERED");
    }

    private NotificationDeliveryDispatcher dispatcherWithOnlyInAppStrategy() {
        return dispatcherWithOnlyInAppStrategy(3);
    }

    private NotificationDeliveryDispatcher dispatcherWithOnlyInAppStrategy(int maxAttempts) {
        NotificationDispatcherProperties properties = new NotificationDispatcherProperties();
        properties.setBatchSize(10);
        properties.setMaxAttempts(maxAttempts);
        properties.setWorkerId("unsupported-channel-test-worker");
        return new NotificationDeliveryDispatcher(
                jdbcTemplate,
                new NotificationChannelRegistry(List.of(new InAppNotificationChannelStrategy())),
                channelProxy,
                transactionTemplate,
                properties
        );
    }

    private void insertFailedAttempt(Long deliveryId, int attemptNumber, String errorMessage) {
        jdbcTemplate.getJdbcTemplate().update("""
                insert into notification_delivery_attempt (
                    delivery_id,
                    attempt_number,
                    attempt_status,
                    error_code,
                    error_message,
                    attempted_at,
                    duration_ms
                )
                values (?, ?, 'FAILED', 'CHANNEL_EXCEPTION', ?, now() - interval '2 minutes', 5)
                """, deliveryId, attemptNumber, errorMessage);
    }

    private DeliveryFixture createPendingEmailAndInAppDeliveries(String eventId) {
        Long accountId = jdbcTemplate.getJdbcTemplate().queryForObject("""
                insert into account (account_state, created_at)
                values ('ACTIVE'::account_state_enum, now())
                returning account_id
                """, Long.class);
        Long notificationId = jdbcTemplate.getJdbcTemplate().queryForObject("""
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
                values (?, 'UnsupportedChannelProbeEvent', 'UNSUPPORTED_CHANNEL_PROBE', 'SYSTEM',
                        'ACCOUNT', ?, 'Unsupported channel probe',
                        'Testing persisted unsupported notification delivery.', '{}'::jsonb, now(), now())
                returning notification_id
                """, Long.class, eventId, accountId);
        Long recipientId = jdbcTemplate.getJdbcTemplate().queryForObject("""
                insert into notification_recipient (
                    notification_id,
                    account_id,
                    recipient_type,
                    recipient_delivery_status,
                    read_status,
                    occurred_at,
                    is_deleted
                )
                values (?, ?, 'ACCOUNT', 'PENDING'::recipient_delivery_status_enum,
                        'UNREAD'::read_status_enum, now(), false)
                returning recipient_id
                """, Long.class, notificationId, accountId);
        Long emailDeliveryId = insertPendingDelivery(recipientId, "EMAIL");
        Long inAppDeliveryId = insertPendingDelivery(recipientId, "IN_APP");
        return new DeliveryFixture(recipientId, emailDeliveryId, inAppDeliveryId);
    }

    private Long insertPendingDelivery(Long recipientId, String channelType) {
        return jdbcTemplate.getJdbcTemplate().queryForObject("""
                insert into notification_delivery (
                    recipient_id,
                    channel_type,
                    channel_delivery_status,
                    attempt_count
                )
                values (?, ?::channel_type_enum, 'PENDING'::channel_delivery_status_enum, 0)
                returning delivery_id
                """, Long.class, recipientId, channelType);
    }

    private record DeliveryFixture(Long recipientId, Long emailDeliveryId, Long inAppDeliveryId) {
    }
}
