package com.project.optrabidz.notification.application.rule;

import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinanceNotificationRuleTest {
    private static final Instant NOW = Instant.parse("2026-09-29T00:00:00Z");

    @Test
    void createsOneOverduePlanForStartupAndInvestor() {
        NotificationRecipientResolver resolver = mock(NotificationRecipientResolver.class);
        when(resolver.accountByStartupId(401L)).thenReturn(Optional.of(501L));
        when(resolver.accountByInvestorId(402L)).thenReturn(Optional.of(502L));
        FinanceNotificationRule rule = new FinanceNotificationRule(JsonMapper.builder().build(), resolver);
        OutboxEvent event = overdueEvent();

        assertThat(rule.supports(event)).isTrue();
        assertThat(rule.createPlans(event))
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.notificationName()).isEqualTo("REPAYMENT_INSTALLMENT_OVERDUE");
                    assertThat(plan.entityType()).isEqualTo("REPAYMENT_INSTALLMENT");
                    assertThat(plan.entityId()).isEqualTo(101L);
                    assertThat(plan.title()).isEqualTo("Repayment installment overdue");
                    assertThat(plan.body()).contains("overdue").contains("take action");
                    assertThat(plan.recipientAccountIds()).containsExactlyInAnyOrder(501L, 502L);
                });
    }

    private OutboxEvent overdueEvent() {
        RepaymentInstallmentOverdueEvent event = new RepaymentInstallmentOverdueEvent(
                101L, 201L, 301L, 401L, 402L, null, null,
                RepaymentInstallmentOverdueSource.SCHEDULE,
                "Repayment installment due date passed",
                NOW
        );
        return OutboxEvent.from(
                event,
                "overdue-1",
                "FINANCIAL",
                "REPAYMENT_INSTALLMENT",
                "101",
                """
                        {
                          "repaymentInstallmentId": 101,
                          "repaymentId": 201,
                          "agreementId": 301,
                          "startupId": 401,
                          "investorId": 402,
                          "paymentIntentId": null,
                          "actorAccountId": null,
                          "source": "SCHEDULE",
                          "reason": "Repayment installment due date passed"
                        }
                        """,
                NOW
        );
    }
}
