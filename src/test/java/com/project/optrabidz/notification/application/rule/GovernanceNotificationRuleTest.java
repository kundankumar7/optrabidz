package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.governance.application.admin.event.AdminAuthorityTransferredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceNotificationRuleTest {
    private static final Instant NOW = Instant.parse("2026-05-26T00:00:00Z");

    private final GovernanceNotificationRule rule = new GovernanceNotificationRule(new ObjectMapper());

    @Test
    void createsSeparateNotificationsForNewAndRevokedAdmin() {
        OutboxEvent event = OutboxEvent.from(
                new AdminAuthorityTransferredEvent(11L, 22L, 33L, "rotation", NOW),
                "admin-transfer-1",
                "GOVERNANCE",
                "ADMIN_AUTHORITY",
                "11",
                """
                        {
                          "newAdminAccountId": 11,
                          "revokedAdminAccountId": 22,
                          "revokedByAccountId": 33,
                          "revocationReason": "rotation"
                        }
                        """,
                NOW
        );

        assertThat(rule.supports(event)).isTrue();
        assertThat(rule.createPlans(event))
                .extracting(NotificationPlan::notificationName)
                .containsExactly("ADMIN_AUTHORITY_GRANTED", "ADMIN_AUTHORITY_REVOKED");
        assertThat(rule.createPlans(event))
                .extracting(NotificationPlan::recipientAccountIds)
                .containsExactlyInAnyOrder(
                        List.of(11L),
                        List.of(22L)
                );
    }
}
