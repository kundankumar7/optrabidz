package com.project.optrabidz.notification.application.channel;

import com.project.optrabidz.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeliveryDispatcherConcurrencyIT extends PostgresIntegrationTestSupport {
    private static final int DELIVERY_COUNT = 30;
    private static final int WORKER_COUNT = 6;

    @Autowired
    private NotificationDeliveryDispatcher dispatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void parallelDispatchersDeliverEachPendingChannelExactlyOnce() throws Exception {
        Long accountId = createAccountWithEmailSubscription();
        List<Long> deliveryIds = createPendingEmailDeliveries(accountId, DELIVERY_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int index = 0; index < WORKER_COUNT; index++) {
                tasks.add(dispatcher::dispatchReadyDeliveries);
            }

            int processed = 0;
            for (Future<Integer> future : executor.invokeAll(tasks)) {
                processed += future.get();
            }

            assertThat(processed).isEqualTo(DELIVERY_COUNT);
            assertDeliveryRowsAreDelivered(deliveryIds);
            assertAttemptHistoryIsExactlyOnce(deliveryIds);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isTrue();
        }
    }

    private Long createAccountWithEmailSubscription() {
        Long accountId = jdbcTemplate.queryForObject("""
                insert into account (account_state, created_at)
                values ('ACTIVE'::account_state_enum, now())
                returning account_id
                """, Long.class);

        jdbcTemplate.update("""
                insert into notification_subscription (
                    account_id,
                    channel_type,
                    endpoint,
                    subscription_state,
                    created_at,
                    updated_at
                )
                values (
                    ?,
                    'EMAIL'::channel_type_enum,
                    'concurrency-notification@example.com',
                    'ACTIVE',
                    now(),
                    now()
                )
                """, accountId);
        return accountId;
    }

    private List<Long> createPendingEmailDeliveries(Long accountId, int count) {
        List<Long> deliveryIds = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Long notificationId = jdbcTemplate.queryForObject("""
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
                        ?,
                        'NotificationConcurrencyProbeEvent',
                        'CONCURRENCY_PROBE',
                        'SYSTEM',
                        'ACCOUNT',
                        ?,
                        'Concurrency probe',
                        'Testing notification delivery concurrency.',
                        '{}'::jsonb,
                        now(),
                        now()
                    )
                    returning notification_id
                    """, Long.class, "concurrency-probe-" + index, accountId);

            Long recipientId = jdbcTemplate.queryForObject("""
                    insert into notification_recipient (
                        notification_id,
                        account_id,
                        recipient_type,
                        recipient_delivery_status,
                        read_status,
                        occurred_at,
                        is_deleted
                    )
                    values (
                        ?,
                        ?,
                        'ACCOUNT',
                        'PENDING'::recipient_delivery_status_enum,
                        'UNREAD'::read_status_enum,
                        now(),
                        false
                    )
                    returning recipient_id
                    """, Long.class, notificationId, accountId);

            Long deliveryId = jdbcTemplate.queryForObject("""
                    insert into notification_delivery (
                        recipient_id,
                        channel_type,
                        channel_delivery_status,
                        attempt_count
                    )
                    values (
                        ?,
                        'EMAIL'::channel_type_enum,
                        'PENDING'::channel_delivery_status_enum,
                        0
                    )
                    returning delivery_id
                    """, Long.class, recipientId);
            deliveryIds.add(deliveryId);
        }
        return deliveryIds;
    }

    private void assertDeliveryRowsAreDelivered(List<Long> deliveryIds) {
        Map<String, Object> stats = jdbcTemplate.queryForMap("""
                select
                    count(*) as total_count,
                    count(*) filter (
                        where channel_delivery_status = 'DELIVERED'::channel_delivery_status_enum
                    ) as delivered_count,
                    min(attempt_count) as min_attempt_count,
                    max(attempt_count) as max_attempt_count
                from notification_delivery
                where delivery_id = any (?)
                """, (Object) deliveryIds.toArray(Long[]::new));
        assertThat(stats.get("total_count")).isEqualTo((long) deliveryIds.size());
        assertThat(stats.get("delivered_count")).isEqualTo((long) deliveryIds.size());
        assertThat(stats.get("min_attempt_count")).isEqualTo(1);
        assertThat(stats.get("max_attempt_count")).isEqualTo(1);
    }

    private void assertAttemptHistoryIsExactlyOnce(List<Long> deliveryIds) {
        Map<String, Object> stats = jdbcTemplate.queryForMap("""
                select
                    count(*) as attempt_count,
                    count(distinct delivery_id) as distinct_delivery_count,
                    min(attempt_number) as min_attempt_number,
                    max(attempt_number) as max_attempt_number
                from notification_delivery_attempt
                where delivery_id = any (?)
                  and attempt_status = 'DELIVERED'
                """, (Object) deliveryIds.toArray(Long[]::new));
        assertThat(stats.get("attempt_count")).isEqualTo((long) deliveryIds.size());
        assertThat(stats.get("distinct_delivery_count")).isEqualTo((long) deliveryIds.size());
        assertThat(stats.get("min_attempt_number")).isEqualTo(1);
        assertThat(stats.get("max_attempt_number")).isEqualTo(1);
    }
}
