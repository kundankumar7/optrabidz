package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;

final class JsonEventPayload {
    private JsonEventPayload() {
    }

    static JsonNode read(ObjectMapper objectMapper, OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception exception) {
            throw new IllegalStateException("Outbox payload could not be parsed for notification", exception);
        }
    }

    static Long longValue(JsonNode jsonNode, String field) {
        JsonNode value = jsonNode.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    static String textValue(JsonNode jsonNode, String field) {
        JsonNode value = jsonNode.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
