package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FinanceAuditPolicy implements AuditPolicy {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "SettlementConfirmedEvent",
            "RepaymentInstallmentPaidEvent",
            "RepaymentInstallmentPaymentFailedEvent"
    );

    private final ObjectMapper objectMapper;

    public FinanceAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return SUPPORTED_EVENTS.contains(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        return switch (event.getEventType()) {
            case "SettlementConfirmedEvent" -> settlementConfirmed(payload);
            case "RepaymentInstallmentPaidEvent" -> repaymentInstallmentPaid(payload);
            case "RepaymentInstallmentPaymentFailedEvent" -> repaymentInstallmentPaymentFailed(payload);
            default -> throw new IllegalStateException("Unsupported finance audit event: " + event.getEventType());
        };
    }

    private AuditDescriptor settlementConfirmed(JsonNode payload) {
        Long settlementId = JsonAuditPayload.longValue(payload, "settlementId");
        return AuditDescriptor.success(
                "FINANCIAL",
                "SETTLEMENT_CONFIRMED",
                "SETTLEMENT",
                settlementId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                "INVESTOR",
                commonFinanceDetails(payload, "settlementId", settlementId)
        );
    }

    private AuditDescriptor repaymentInstallmentPaid(JsonNode payload) {
        Long repaymentInstallmentId = JsonAuditPayload.longValue(payload, "repaymentInstallmentId");
        return AuditDescriptor.success(
                "FINANCIAL",
                "REPAYMENT_INSTALLMENT_PAID",
                "REPAYMENT_INSTALLMENT",
                repaymentInstallmentId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                "STARTUP",
                commonFinanceDetails(payload, "repaymentInstallmentId", repaymentInstallmentId)
        );
    }

    private AuditDescriptor repaymentInstallmentPaymentFailed(JsonNode payload) {
        Long repaymentInstallmentId = JsonAuditPayload.longValue(payload, "repaymentInstallmentId");
        return AuditDescriptor.failed(
                "FINANCIAL",
                "REPAYMENT_INSTALLMENT_PAYMENT_FAILED",
                "REPAYMENT_INSTALLMENT",
                repaymentInstallmentId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                "STARTUP",
                commonFinanceDetails(payload, "repaymentInstallmentId", repaymentInstallmentId)
        );
    }

    private java.util.Map<String, Object> commonFinanceDetails(JsonNode payload,
                                                               String primaryKey,
                                                               Long primaryValue) {
        return JsonAuditPayload.details(
                primaryKey, primaryValue,
                "repaymentId", JsonAuditPayload.longValue(payload, "repaymentId"),
                "agreementId", JsonAuditPayload.longValue(payload, "agreementId"),
                "startupId", JsonAuditPayload.longValue(payload, "startupId"),
                "investorId", JsonAuditPayload.longValue(payload, "investorId"),
                "paymentIntentId", JsonAuditPayload.longValue(payload, "paymentIntentId"),
                "reason", JsonAuditPayload.textValue(payload, "reason")
        );
    }
}
