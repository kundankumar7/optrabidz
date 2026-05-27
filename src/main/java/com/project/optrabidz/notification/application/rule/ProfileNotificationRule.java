package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProfileNotificationRule implements NotificationRule {
    private static final Map<String, String> NOTIFICATION_NAMES = Map.of(
            "ParticipationProfileChangedEvent", "PARTICIPATION_PROFILE_CHANGED",
            "StartupClassificationChangedEvent", "STARTUP_CLASSIFICATION_CHANGED",
            "InvestorPreferenceChangedEvent", "INVESTOR_PREFERENCE_CHANGED"
    );
    private static final Set<String> SUPPORTED_EVENTS = NOTIFICATION_NAMES.keySet();

    private final ObjectMapper objectMapper;

    public ProfileNotificationRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return SUPPORTED_EVENTS.contains(event.getEventType());
    }

    @Override
    public List<NotificationPlan> createPlans(OutboxEvent event) {
        JsonNode payload = JsonEventPayload.read(objectMapper, event);
        Long accountId = JsonEventPayload.longValue(payload, "accountId");
        if (accountId == null) {
            return List.of();
        }
        return List.of(new NotificationPlan(
                NOTIFICATION_NAMES.get(event.getEventType()),
                "PROFILE",
                "ACCOUNT",
                accountId,
                "Profile updated",
                "Your profile information was updated.",
                event.getPayload(),
                event.getOccurredAt(),
                List.of(accountId),
                NotificationChannels.standard()
        ));
    }
}
