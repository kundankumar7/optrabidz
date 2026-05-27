package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FinanceNotificationRule implements NotificationRule {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "SettlementConfirmedEvent",
            "RepaymentInstallmentPaidEvent",
            "RepaymentInstallmentPaymentFailedEvent"
    );

    private final ObjectMapper objectMapper;
    private final NotificationRecipientResolver recipientResolver;

    public FinanceNotificationRule(ObjectMapper objectMapper,
                                   NotificationRecipientResolver recipientResolver) {
        this.objectMapper = objectMapper;
        this.recipientResolver = recipientResolver;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return SUPPORTED_EVENTS.contains(event.getEventType());
    }

    @Override
    public List<NotificationPlan> createPlans(OutboxEvent event) {
        JsonNode payload = JsonEventPayload.read(objectMapper, event);
        return switch (event.getEventType()) {
            case "SettlementConfirmedEvent" -> financePlan(
                    event,
                    payload,
                    "SETTLEMENT_CONFIRMED",
                    "FINANCE",
                    "SETTLEMENT",
                    JsonEventPayload.longValue(payload, "settlementId"),
                    "Settlement confirmed",
                    "Investor settlement payment has been confirmed."
            );
            case "RepaymentInstallmentPaidEvent" -> financePlan(
                    event,
                    payload,
                    "REPAYMENT_INSTALLMENT_PAID",
                    "FINANCE",
                    "REPAYMENT_INSTALLMENT",
                    JsonEventPayload.longValue(payload, "repaymentInstallmentId"),
                    "Repayment installment paid",
                    "A repayment installment has been paid successfully."
            );
            case "RepaymentInstallmentPaymentFailedEvent" -> financePlan(
                    event,
                    payload,
                    "REPAYMENT_INSTALLMENT_PAYMENT_FAILED",
                    "FINANCE",
                    "REPAYMENT_INSTALLMENT",
                    JsonEventPayload.longValue(payload, "repaymentInstallmentId"),
                    "Repayment payment failed",
                    "A repayment installment payment attempt failed and needs attention."
            );
            default -> List.of();
        };
    }

    private List<NotificationPlan> financePlan(OutboxEvent event,
                                               JsonNode payload,
                                               String name,
                                               String type,
                                               String entityType,
                                               Long entityId,
                                               String title,
                                               String body) {
        List<Long> recipients = new ArrayList<>();
        recipientResolver.accountByStartupId(JsonEventPayload.longValue(payload, "startupId"))
                .ifPresent(recipients::add);
        recipientResolver.accountByInvestorId(JsonEventPayload.longValue(payload, "investorId"))
                .ifPresent(recipients::add);
        if (recipients.isEmpty()) {
            return List.of();
        }
        return List.of(new NotificationPlan(
                name,
                type,
                entityType,
                entityId,
                title,
                body,
                event.getPayload(),
                event.getOccurredAt(),
                recipients,
                NotificationChannels.standard()
        ));
    }
}
