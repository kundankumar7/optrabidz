package com.project.optrabidz.common.outbox;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxDispatcherRetryIT extends PostgresIntegrationTestSupport {
    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void failedProcessorKeepsEventPendingAndSchedulesRetry() {
        eventPublisher.publish(new RetryProbeEvent(9001L, Instant.now()));

        int processed = outboxDispatcher.dispatchPending();

        assertThat(processed).isEqualTo(1);
        Map<String, Object> outbox = jdbcTemplate.queryForMap("""
                select event_status, retry_count, last_error, available_at
                from event_outbox
                where event_type = 'RetryProbeEvent'
                order by outbox_event_id desc
                limit 1
                """);

        assertThat(outbox.get("event_status")).isEqualTo("PENDING");
        assertThat(outbox.get("retry_count")).isEqualTo(1);
        assertThat(outbox.get("last_error").toString()).contains("planned retry probe failure");
        assertThat(outbox.get("available_at")).isNotNull();
    }

    record RetryProbeEvent(Long accountId, Instant occurredAt) implements DomainEvent {
    }

    @TestConfiguration
    static class RetryProbeProcessorConfiguration {
        @Bean
        OutboxEventProcessor retryProbeFailingProcessor() {
            return new OutboxEventProcessor() {
                @Override
                public boolean supports(OutboxEvent event) {
                    return "RetryProbeEvent".equals(event.getEventType());
                }

                @Override
                public void process(OutboxEvent event) {
                    throw new IllegalStateException("planned retry probe failure");
                }
            };
        }
    }
}
