package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.classification.application.event.InvestorPreferenceChangedEvent;
import com.project.optrabidz.classification.application.event.StartupClassificationChangedEvent;
import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileNotificationRuleTest {
    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");

    private final ProfileNotificationRule rule = new ProfileNotificationRule(new ObjectMapper());

    @Test
    void usesCanonicalNotificationNamesForProfileEvents() {
        assertNotificationName(
                new ParticipationProfileChangedEvent(10L, RoleType.STARTUP, NOW),
                "PARTICIPATION_PROFILE_CHANGED"
        );
        assertNotificationName(
                new StartupClassificationChangedEvent(20L, 10L, NOW),
                "STARTUP_CLASSIFICATION_CHANGED"
        );
        assertNotificationName(
                new InvestorPreferenceChangedEvent(30L, 10L, NOW),
                "INVESTOR_PREFERENCE_CHANGED"
        );
    }

    private void assertNotificationName(DomainEvent domainEvent, String expectedNotificationName) {
        OutboxEvent event = OutboxEvent.from(
                domainEvent,
                domainEvent.getClass().getSimpleName() + "-1",
                "TEST",
                "ACCOUNT",
                "10",
                "{\"accountId\":10}",
                NOW
        );

        assertThat(rule.supports(event)).isTrue();
        assertThat(rule.createPlans(event))
                .singleElement()
                .extracting(NotificationPlan::notificationName)
                .isEqualTo(expectedNotificationName);
    }
}
