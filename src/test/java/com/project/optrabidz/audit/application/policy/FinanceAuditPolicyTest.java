package com.project.optrabidz.audit.application.policy;

import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.outbox.OutboxEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingCreatedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingDeactivatedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingReplacedEvent;
import com.project.optrabidz.financial.application.event.PaymentAccountBindingVerifiedEvent;
import com.project.optrabidz.financial.application.event.PayoutTransferFailedEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueEvent;
import com.project.optrabidz.financial.application.event.RepaymentInstallmentOverdueSource;
import com.project.optrabidz.identity.domain.model.RoleType;
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

    @Test
    void mapsPaymentAccountBindingLifecycleWithoutSensitiveDetails() {
        OutboxEvent created = OutboxEvent.from(
                new PaymentAccountBindingCreatedEvent(11L, 501L, RoleType.STARTUP, NOW),
                "binding-created", "FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "11",
                """
                        {"paymentAccountBindingId":11,"actorAccountId":501,"actorRole":"STARTUP"}
                        """, NOW);
        OutboxEvent verified = OutboxEvent.from(
                new PaymentAccountBindingVerifiedEvent(11L, 501L, RoleType.STARTUP, NOW),
                "binding-verified", "FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "11",
                """
                        {"paymentAccountBindingId":11,"actorAccountId":501,"actorRole":"STARTUP"}
                        """, NOW);
        OutboxEvent replaced = OutboxEvent.from(
                new PaymentAccountBindingReplacedEvent(11L, 12L, 601L, RoleType.INVESTOR, NOW),
                "binding-replaced", "FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "12",
                """
                        {"previousPaymentAccountBindingId":11,"replacementPaymentAccountBindingId":12,
                         "actorAccountId":601,"actorRole":"INVESTOR"}
                        """, NOW);
        OutboxEvent deactivated = OutboxEvent.from(
                new PaymentAccountBindingDeactivatedEvent(12L, 601L, RoleType.INVESTOR, NOW),
                "binding-deactivated", "FINANCIAL", "PAYMENT_ACCOUNT_BINDING", "12",
                """
                        {"paymentAccountBindingId":12,"actorAccountId":601,"actorRole":"INVESTOR"}
                        """, NOW);

        assertThat(policy.supports(created)).isTrue();
        assertThat(policy.describe(created))
                .extracting(AuditDescriptor::action, AuditDescriptor::objectType,
                        AuditDescriptor::objectId, AuditDescriptor::actorRole)
                .containsExactly("PAYMENT_ACCOUNT_BINDING_CREATED", "PAYMENT_ACCOUNT_BINDING", "11", "STARTUP");
        assertThat(policy.describe(verified).action()).isEqualTo("PAYMENT_ACCOUNT_BINDING_VERIFIED");
        assertThat(policy.describe(replaced).details())
                .containsOnly(
                        org.assertj.core.api.Assertions.entry("previousPaymentAccountBindingId", 11L),
                        org.assertj.core.api.Assertions.entry("replacementPaymentAccountBindingId", 12L)
                );
        assertThat(policy.describe(deactivated))
                .extracting(AuditDescriptor::action, AuditDescriptor::actorAccountId,
                        AuditDescriptor::actorRole, AuditDescriptor::outcome)
                .containsExactly("PAYMENT_ACCOUNT_BINDING_DEACTIVATED", 601L,
                        "INVESTOR", AuditOutcome.SUCCESS);
    }

    @Test
    void mapsPayoutFailureToSystemFailureAudit() {
        PayoutTransferFailedEvent domainEvent = new PayoutTransferFailedEvent(
                41L, 31L, 501L, 502L, 1,
                "DEMO_PAYOUT_FAILED", "Demonstration payout failed", NOW);
        OutboxEvent event = OutboxEvent.from(
                domainEvent,
                "payout-failed",
                "FINANCIAL",
                "PAYOUT_TRANSFER",
                "41",
                """
                        {"payoutTransferId":41,"paymentIntentId":31,
                         "payerAccountId":501,"payeeAccountId":502,"attemptCount":1,
                         "failureCode":"DEMO_PAYOUT_FAILED",
                         "failureMessage":"Demonstration payout failed"}
                        """,
                NOW
        );

        assertThat(policy.supports(event)).isTrue();
        assertThat(policy.describe(event))
                .extracting(AuditDescriptor::action, AuditDescriptor::objectType,
                        AuditDescriptor::objectId, AuditDescriptor::actorRole,
                        AuditDescriptor::outcome)
                .containsExactly("PAYOUT_TRANSFER_FAILED", "PAYOUT_TRANSFER", "41",
                        "SYSTEM", AuditOutcome.FAILED);
        assertThat(policy.describe(event).details())
                .containsEntry("paymentIntentId", 31L)
                .containsEntry("attemptCount", 1L)
                .containsEntry("failureCode", "DEMO_PAYOUT_FAILED");
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
