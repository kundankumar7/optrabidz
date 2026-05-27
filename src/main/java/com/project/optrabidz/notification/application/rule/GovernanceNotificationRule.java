package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GovernanceNotificationRule implements NotificationRule {
    private static final String ADMIN_AUTHORITY_TRANSFERRED_EVENT = "AdminAuthorityTransferredEvent";

    private final ObjectMapper objectMapper;

    public GovernanceNotificationRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return ADMIN_AUTHORITY_TRANSFERRED_EVENT.equals(event.getEventType());
    }

    @Override
    public List<NotificationPlan> createPlans(OutboxEvent event) {
        JsonNode payload = JsonEventPayload.read(objectMapper, event);
        Long newAdminAccountId = JsonEventPayload.longValue(payload, "newAdminAccountId");
        Long revokedAdminAccountId = JsonEventPayload.longValue(payload, "revokedAdminAccountId");

        List<NotificationPlan> plans = new ArrayList<>();
        if (newAdminAccountId != null) {
            plans.add(plan(
                    event,
                    "ADMIN_AUTHORITY_GRANTED",
                    newAdminAccountId,
                    "Admin authority granted",
                    "You are now the active OptraBidz admin."
            ));
        }
        if (revokedAdminAccountId != null) {
            plans.add(plan(
                    event,
                    "ADMIN_AUTHORITY_REVOKED",
                    revokedAdminAccountId,
                    "Admin authority revoked",
                    "You are no longer the active OptraBidz admin."
            ));
        }
        return plans;
    }

    private NotificationPlan plan(OutboxEvent event,
                                  String name,
                                  Long accountId,
                                  String title,
                                  String body) {
        return new NotificationPlan(
                name,
                "GOVERNANCE",
                "ADMIN_AUTHORITY",
                accountId,
                title,
                body,
                event.getPayload(),
                event.getOccurredAt(),
                List.of(accountId),
                NotificationChannels.standard()
        );
    }
}
