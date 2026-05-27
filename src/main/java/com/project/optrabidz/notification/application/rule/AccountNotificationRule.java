package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountNotificationRule implements NotificationRule {
    private final ObjectMapper objectMapper;

    public AccountNotificationRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "AccountRegisteredEvent".equals(event.getEventType());
    }

    @Override
    public List<NotificationPlan> createPlans(OutboxEvent event) {
        JsonNode payload = JsonEventPayload.read(objectMapper, event);
        Long accountId = JsonEventPayload.longValue(payload, "accountId");
        if (accountId == null) {
            return List.of();
        }
        return List.of(new NotificationPlan(
                "ACCOUNT_REGISTERED",
                "ACCOUNT",
                "ACCOUNT",
                accountId,
                "Welcome to OptraBidz",
                "Your account has been created successfully.",
                event.getPayload(),
                event.getOccurredAt(),
                List.of(accountId),
                NotificationChannels.standard()
        ));
    }
}
