package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceAuditPolicyTest {
    private static final Instant NOW = Instant.parse("2026-09-29T00:00:00Z");

    private final FinanceAuditPolicy policy = new FinanceAuditPolicy(JsonMapper.builder().build());

    @Test
    void mapsScheduledOverdueTransitionToSystemAudit() {
        AuditDescriptor descriptor = policy.describe(overdueEvent(
                null,
                RepaymentInstallmentOverdueSource.SCHEDULE,
                "Repayment installment due date passed"
        ));

        assertThat(descriptor.action()).isEqualTo("REPAYMENT_INSTALLMENT_OVERDUE");
        assertThat(descriptor.objectType()).isEqualTo("REPAYMENT_INSTALLMENT");
        assertThat(descriptor.objectId()).isEqualTo("101");
        assertThat(descriptor.actorAccountId()).isNull();
        assertThat(descriptor.actorRole()).isEqualTo("SYSTEM");
        assertThat(descriptor.outcome()).isEqualTo(AuditOutcome.SYSTEM);
        assertThat(descriptor.details())
                .containsEntry("source", "SCHEDULE")
                .containsEntry("reason", "Repayment installment due date passed");
    }

    @Test
    void mapsPaymentFailureOverdueTransitionToStartupFailureAudit() {
        AuditDescriptor descriptor = policy.describe(overdueEvent(
                501L,
                RepaymentInstallmentOverdueSource.PAYMENT_FAILURE,
                "Payment declined"
        ));

        assertThat(descriptor.actorAccountId()).isEqualTo(501L);
        assertThat(descriptor.actorRole()).isEqualTo("STARTUP");
        assertThat(descriptor.outcome()).isEqualTo(AuditOutcome.FAILED);
        assertThat(descriptor.details()).containsEntry("source", "PAYMENT_FAILURE");
    }

    private OutboxEvent overdueEvent(Long actorAccountId,
                                     RepaymentInstallmentOverdueSource source,
                                     String reason) {
        RepaymentInstallmentOverdueEvent event = new RepaymentInstallmentOverdueEvent(
                101L, 201L, 301L, 401L, 402L, 601L,
                actorAccountId, source, reason, NOW
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
                          "paymentIntentId": 601,
                          "actorAccountId": %s,
                          "source": "%s",
                          "reason": "%s"
                        }
                        """.formatted(actorAccountId == null ? "null" : actorAccountId, source, reason),
                NOW
        );
    }
}
