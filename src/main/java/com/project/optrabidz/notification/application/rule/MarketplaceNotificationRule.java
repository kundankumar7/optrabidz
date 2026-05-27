package com.project.optrabidz.notification.application.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class MarketplaceNotificationRule implements NotificationRule {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "ListingPublishedEvent",
            "ListingClosedEvent",
            "BidSubmittedEvent",
            "BidWithdrawnEvent",
            "BidAcceptedEvent",
            "BidRejectedEvent",
            "AgreementCreatedEvent"
    );

    private final ObjectMapper objectMapper;
    private final NotificationRecipientResolver recipientResolver;

    public MarketplaceNotificationRule(ObjectMapper objectMapper,
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
            case "ListingPublishedEvent" -> listingOwnerPlan(event, payload, "LISTING_PUBLISHED", "Listing published", "Your funding listing is now visible to investors.");
            case "ListingClosedEvent" -> listingOwnerPlan(event, payload, "LISTING_CLOSED", "Listing closed", "Your funding listing has been closed.");
            case "BidSubmittedEvent" -> listingOwnerPlan(event, payload, "BID_SUBMITTED", "New bid received", "An investor submitted a bid on your listing.");
            case "BidWithdrawnEvent" -> listingOwnerPlan(event, payload, "BID_WITHDRAWN", "Bid withdrawn", "An investor withdrew a bid from your listing.");
            case "BidAcceptedEvent" -> investorPlan(event, payload, "BID_ACCEPTED", "Bid accepted", "A startup accepted your bid.");
            case "BidRejectedEvent" -> investorPlan(event, payload, "BID_REJECTED", "Bid rejected", "A startup rejected your bid.");
            case "AgreementCreatedEvent" -> agreementPlans(event, payload);
            default -> List.of();
        };
    }

    private List<NotificationPlan> listingOwnerPlan(OutboxEvent event,
                                                    JsonNode payload,
                                                    String name,
                                                    String title,
                                                    String body) {
        Long listingId = JsonEventPayload.longValue(payload, "listingId");
        Optional<Long> accountId = recipientResolver.startupAccountByListingId(listingId);
        return accountId.map(id -> List.of(plan(event, name, "MARKETPLACE", "FUNDING_LISTING", listingId, title, body, List.of(id))))
                .orElseGet(List::of);
    }

    private List<NotificationPlan> investorPlan(OutboxEvent event,
                                                JsonNode payload,
                                                String name,
                                                String title,
                                                String body) {
        Long investorId = JsonEventPayload.longValue(payload, "investorId");
        Optional<Long> accountId = recipientResolver.accountByInvestorId(investorId);
        Long bidId = JsonEventPayload.longValue(payload, "bidId");
        return accountId.map(id -> List.of(plan(event, name, "MARKETPLACE", "BID", bidId, title, body, List.of(id))))
                .orElseGet(List::of);
    }

    private List<NotificationPlan> agreementPlans(OutboxEvent event, JsonNode payload) {
        List<Long> recipients = new ArrayList<>();
        Long startupId = JsonEventPayload.longValue(payload, "startupId");
        Long investorId = JsonEventPayload.longValue(payload, "investorId");
        recipientResolver.accountByStartupId(startupId).ifPresent(recipients::add);
        recipientResolver.accountByInvestorId(investorId).ifPresent(recipients::add);
        if (recipients.isEmpty()) {
            return List.of();
        }
        Long agreementId = JsonEventPayload.longValue(payload, "agreementId");
        return List.of(plan(event, "AGREEMENT_CREATED", "MARKETPLACE", "AGREEMENT", agreementId,
                "Agreement created", "A funding agreement has been created.", recipients));
    }

    private NotificationPlan plan(OutboxEvent event,
                                  String name,
                                  String type,
                                  String entityType,
                                  Long entityId,
                                  String title,
                                  String body,
                                  List<Long> recipients) {
        return new NotificationPlan(
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
        );
    }
}
