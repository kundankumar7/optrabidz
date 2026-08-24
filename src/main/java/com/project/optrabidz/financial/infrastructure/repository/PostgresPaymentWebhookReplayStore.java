package com.project.optrabidz.financial.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.financial.application.port.PaymentWebhookReplayStore;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayContent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayState;
import com.project.optrabidz.financial.application.replay.StoredPaymentWebhookReplayEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@Repository
public class PostgresPaymentWebhookReplayStore implements PaymentWebhookReplayStore {
    private static final String CLAIM_SQL = """
            insert into payment_webhook_event (
                provider_code,
                provider_event_id,
                event_type,
                payment_intent_id,
                payment_attempt_id,
                processing_state,
                received_at,
                payload_hash,
                payload,
                failure_message
            ) values (
                :providerCode,
                :providerEventId,
                :eventType,
                null,
                null,
                cast('RECEIVED' as payment_webhook_processing_state_enum),
                :receivedAt,
                :payloadHash,
                cast(:payload as jsonb),
                null
            )
            on conflict (provider_code, provider_event_id) do nothing
            returning payment_webhook_event_id
            """;

    private static final String FIND_SQL = """
            select payment_webhook_event_id,
                   processing_state::text as processing_state,
                   payload_hash,
                   payload::text as payload
            from payment_webhook_event
            where provider_code = :providerCode
              and provider_event_id = :providerEventId
            """;

    private static final String COMPLETE_SQL = """
            update payment_webhook_event
            set processing_state = cast('PROCESSED' as payment_webhook_processing_state_enum),
                payment_intent_id = :paymentIntentId,
                payment_attempt_id = :paymentAttemptId,
                processed_at = :processedAt
            where payment_webhook_event_id = :replayEventId
              and processing_state = cast('RECEIVED' as payment_webhook_processing_state_enum)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresPaymentWebhookReplayStore(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public OptionalLong tryClaim(
            PaymentWebhookReplayEvent event,
            Instant receivedAt) {
        PaymentWebhookReplayContent content = event.content();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("providerCode", content.providerCode())
                .addValue("providerEventId", content.providerEventId())
                .addValue("eventType", content.eventType().name())
                .addValue("receivedAt", Timestamp.from(receivedAt))
                .addValue("payloadHash", event.payloadHash())
                .addValue("payload", serialize(content));
        List<Long> ids = jdbcTemplate.query(
                CLAIM_SQL,
                parameters,
                (resultSet, rowNumber) -> resultSet.getLong(
                        "payment_webhook_event_id"
                )
        );
        return ids.isEmpty()
                ? OptionalLong.empty()
                : OptionalLong.of(ids.getFirst());
    }

    @Override
    public Optional<StoredPaymentWebhookReplayEvent> findByIdentity(
            String providerCode,
            String providerEventId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("providerCode", providerCode)
                .addValue("providerEventId", providerEventId);
        List<StoredPaymentWebhookReplayEvent> events = jdbcTemplate.query(
                FIND_SQL,
                parameters,
                (resultSet, rowNumber) -> new StoredPaymentWebhookReplayEvent(
                        resultSet.getLong("payment_webhook_event_id"),
                        PaymentWebhookReplayState.valueOf(
                                resultSet.getString("processing_state")
                        ),
                        new PaymentWebhookReplayEvent(
                                deserialize(resultSet.getString("payload")),
                                resultSet.getString("payload_hash")
                        )
                )
        );
        return events.stream().findFirst();
    }

    @Override
    public void markProcessed(
            long replayEventId,
            long paymentIntentId,
            long paymentAttemptId,
            Instant processedAt) {
        int updated = jdbcTemplate.update(
                COMPLETE_SQL,
                new MapSqlParameterSource()
                        .addValue("replayEventId", replayEventId)
                        .addValue("paymentIntentId", paymentIntentId)
                        .addValue("paymentAttemptId", paymentAttemptId)
                        .addValue("processedAt", Timestamp.from(processedAt))
        );
        if (updated != 1) {
            throw new IllegalStateException(
                    "Payment webhook replay completion invariant failed"
            );
        }
    }

    private String serialize(PaymentWebhookReplayContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Payment webhook replay content could not be serialized",
                    exception
            );
        }
    }

    private PaymentWebhookReplayContent deserialize(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    PaymentWebhookReplayContent.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Payment webhook replay content could not be deserialized",
                    exception
            );
        }
    }
}
