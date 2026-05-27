package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;

import java.util.LinkedHashMap;
import java.util.Map;

final class JsonAuditPayload {
    private JsonAuditPayload() {
    }

    static JsonNode read(ObjectMapper objectMapper, OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    static Long longValue(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        return node.isNumber() ? node.longValue() : null;
    }

    static String textValue(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        return node.isTextual() ? node.textValue() : null;
    }

    static Map<String, Object> details(Object... pairs) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            Object key = pairs[index];
            Object value = pairs[index + 1];
            if (key != null && value != null) {
                details.put(String.valueOf(key), value);
            }
        }
        return details;
    }
}
