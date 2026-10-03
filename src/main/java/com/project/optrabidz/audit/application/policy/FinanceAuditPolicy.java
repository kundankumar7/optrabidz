package com.project.optrabidz.audit.application.policy;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.project.optrabidz.audit.domain.model.AuditOutcome;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FinanceAuditPolicy implements AuditPolicy {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "SettlementConfirmedEvent",
            "RepaymentInstallmentPaidEvent",
            "RepaymentInstallmentPaymentFailedEvent",
            "RepaymentInstallmentOverdueEvent",
            "PaymentAccountBindingCreatedEvent",
            "PaymentAccountBindingVerifiedEvent",
            "PaymentAccountBindingReplacedEvent",
            "PaymentAccountBindingDeactivatedEvent",
            "PaymentCollectionConfirmedEvent",
            "PaymentCollectionCancelledEvent",
            "PayoutTransferCreatedEvent",
            "PayoutTransferRetryStartedEvent",
            "PayoutTransferFailedEvent",
            "PayoutTransferConfirmedEvent"
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
            case "RepaymentInstallmentOverdueEvent" -> repaymentInstallmentOverdue(payload);
            case "PaymentAccountBindingCreatedEvent" -> paymentAccountBinding(
                    payload, "PAYMENT_ACCOUNT_BINDING_CREATED", "paymentAccountBindingId");
            case "PaymentAccountBindingVerifiedEvent" -> paymentAccountBinding(
                    payload, "PAYMENT_ACCOUNT_BINDING_VERIFIED", "paymentAccountBindingId");
            case "PaymentAccountBindingReplacedEvent" -> paymentAccountBindingReplaced(payload);
            case "PaymentAccountBindingDeactivatedEvent" -> paymentAccountBinding(
                    payload, "PAYMENT_ACCOUNT_BINDING_DEACTIVATED", "paymentAccountBindingId");
            case "PaymentCollectionConfirmedEvent" -> paymentCollectionConfirmed(payload);
            case "PaymentCollectionCancelledEvent" -> paymentCollectionCancelled(payload);
            case "PayoutTransferCreatedEvent" -> payoutTransfer(
                    payload, "PAYOUT_TRANSFER_CREATED", AuditOutcome.SUCCESS);
            case "PayoutTransferRetryStartedEvent" -> payoutTransferRetry(payload);
            case "PayoutTransferFailedEvent" -> payoutTransfer(
                    payload, "PAYOUT_TRANSFER_FAILED", AuditOutcome.FAILED);
            case "PayoutTransferConfirmedEvent" -> payoutTransfer(
                    payload, "PAYOUT_TRANSFER_CONFIRMED", AuditOutcome.SUCCESS);
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

    private AuditDescriptor repaymentInstallmentOverdue(JsonNode payload) {
        Long repaymentInstallmentId = JsonAuditPayload.longValue(payload, "repaymentInstallmentId");
        Long actorAccountId = JsonAuditPayload.longValue(payload, "actorAccountId");
        boolean systemDriven = actorAccountId == null;
        return new AuditDescriptor(
                "FINANCIAL",
                "REPAYMENT_INSTALLMENT_OVERDUE",
                "REPAYMENT_INSTALLMENT",
                repaymentInstallmentId == null ? null : String.valueOf(repaymentInstallmentId),
                actorAccountId,
                systemDriven ? "SYSTEM" : "STARTUP",
                systemDriven ? AuditOutcome.SYSTEM : AuditOutcome.FAILED,
                commonFinanceDetails(payload, "repaymentInstallmentId", repaymentInstallmentId)
        );
    }

    private AuditDescriptor paymentAccountBinding(JsonNode payload,
                                                  String action,
                                                  String bindingIdField) {
        Long bindingId = JsonAuditPayload.longValue(payload, bindingIdField);
        return AuditDescriptor.success(
                "FINANCIAL",
                action,
                "PAYMENT_ACCOUNT_BINDING",
                bindingId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                JsonAuditPayload.textValue(payload, "actorRole"),
                JsonAuditPayload.details(bindingIdField, bindingId)
        );
    }

    private AuditDescriptor paymentAccountBindingReplaced(JsonNode payload) {
        Long previousId = JsonAuditPayload.longValue(payload, "previousPaymentAccountBindingId");
        Long replacementId = JsonAuditPayload.longValue(payload, "replacementPaymentAccountBindingId");
        return AuditDescriptor.success(
                "FINANCIAL",
                "PAYMENT_ACCOUNT_BINDING_REPLACED",
                "PAYMENT_ACCOUNT_BINDING",
                replacementId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                JsonAuditPayload.textValue(payload, "actorRole"),
                JsonAuditPayload.details(
                        "previousPaymentAccountBindingId", previousId,
                        "replacementPaymentAccountBindingId", replacementId
                )
        );
    }

    private AuditDescriptor paymentCollectionCancelled(JsonNode payload) {
        Long paymentAttemptId = JsonAuditPayload.longValue(payload, "paymentAttemptId");
        String paymentPurpose = JsonAuditPayload.textValue(payload, "paymentPurpose");
        return AuditDescriptor.success(
                "FINANCIAL",
                "PAYMENT_COLLECTION_CANCELLED",
                "PAYMENT_ATTEMPT",
                paymentAttemptId,
                JsonAuditPayload.longValue(payload, "payerAccountId"),
                "REPAYMENT".equals(paymentPurpose) ? "STARTUP" : "INVESTOR",
                JsonAuditPayload.details(
                        "paymentAttemptId", paymentAttemptId,
                        "paymentIntentId", JsonAuditPayload.longValue(payload, "paymentIntentId"),
                        "paymentPurpose", paymentPurpose,
                        "reason", JsonAuditPayload.textValue(payload, "reason")
                )
        );
    }

    private AuditDescriptor paymentCollectionConfirmed(JsonNode payload) {
        Long paymentIntentId = JsonAuditPayload.longValue(payload, "paymentIntentId");
        return new AuditDescriptor(
                "FINANCIAL",
                "PAYMENT_COLLECTION_CONFIRMED",
                "PAYMENT_INTENT",
                paymentIntentId == null ? null : String.valueOf(paymentIntentId),
                null,
                "SYSTEM",
                AuditOutcome.SYSTEM,
                JsonAuditPayload.details("paymentIntentId", paymentIntentId)
        );
    }

    private AuditDescriptor payoutTransfer(JsonNode payload,
                                           String action,
                                           AuditOutcome outcome) {
        Long payoutTransferId = JsonAuditPayload.longValue(payload, "payoutTransferId");
        return new AuditDescriptor(
                "FINANCIAL",
                action,
                "PAYOUT_TRANSFER",
                payoutTransferId == null ? null : String.valueOf(payoutTransferId),
                null,
                "SYSTEM",
                outcome,
                JsonAuditPayload.details(
                        "paymentIntentId", JsonAuditPayload.longValue(payload, "paymentIntentId"),
                        "attemptCount", JsonAuditPayload.longValue(payload, "attemptCount"),
                        "failureCode", JsonAuditPayload.textValue(payload, "failureCode")
                )
        );
    }

    private AuditDescriptor payoutTransferRetry(JsonNode payload) {
        Long payoutTransferId = JsonAuditPayload.longValue(payload, "payoutTransferId");
        return AuditDescriptor.success(
                "FINANCIAL",
                "PAYOUT_TRANSFER_RETRY_STARTED",
                "PAYOUT_TRANSFER",
                payoutTransferId,
                JsonAuditPayload.longValue(payload, "actorAccountId"),
                JsonAuditPayload.textValue(payload, "actorRole"),
                JsonAuditPayload.details(
                        "paymentIntentId", JsonAuditPayload.longValue(payload, "paymentIntentId"),
                        "attemptCount", JsonAuditPayload.longValue(payload, "attemptCount")
                )
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
                "source", JsonAuditPayload.textValue(payload, "source"),
                "reason", JsonAuditPayload.textValue(payload, "reason")
        );
    }
}
