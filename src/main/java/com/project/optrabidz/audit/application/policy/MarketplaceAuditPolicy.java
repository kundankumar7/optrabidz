package com.project.optrabidz.audit.application.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.optrabidz.common.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MarketplaceAuditPolicy implements AuditPolicy {
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

    public MarketplaceAuditPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return SUPPORTED_EVENTS.contains(event.getEventType());
    }

    @Override
    public AuditDescriptor describe(OutboxEvent event) {
        JsonNode payload = JsonAuditPayload.read(objectMapper, event);
        Long actorAccountId = JsonAuditPayload.longValue(payload, "actorAccountId");
        return switch (event.getEventType()) {
            case "ListingPublishedEvent" -> listingDescriptor(payload, "LISTING_PUBLISHED", actorAccountId);
            case "ListingClosedEvent" -> listingDescriptor(payload, "LISTING_CLOSED", actorAccountId);
            case "BidSubmittedEvent" -> bidDescriptor(payload, "BID_SUBMITTED", actorAccountId, "INVESTOR");
            case "BidWithdrawnEvent" -> bidDescriptor(payload, "BID_WITHDRAWN", actorAccountId, "INVESTOR");
            case "BidAcceptedEvent" -> bidDescriptor(payload, "BID_ACCEPTED", actorAccountId, "STARTUP");
            case "BidRejectedEvent" -> bidDescriptor(payload, "BID_REJECTED", actorAccountId, "STARTUP");
            case "AgreementCreatedEvent" -> agreementDescriptor(payload, actorAccountId);
            default -> throw new IllegalStateException("Unsupported marketplace audit event: " + event.getEventType());
        };
    }

    private AuditDescriptor listingDescriptor(JsonNode payload, String action, Long actorAccountId) {
        Long listingId = JsonAuditPayload.longValue(payload, "listingId");
        Long startupId = JsonAuditPayload.longValue(payload, "startupId");
        return AuditDescriptor.success(
                "MARKETPLACE",
                action,
                "FUNDING_LISTING",
                listingId,
                actorAccountId,
                "STARTUP",
                JsonAuditPayload.details(
                        "listingId", listingId,
                        "startupId", startupId,
                        "reason", JsonAuditPayload.textValue(payload, "reason")
                )
        );
    }

    private AuditDescriptor bidDescriptor(JsonNode payload,
                                          String action,
                                          Long actorAccountId,
                                          String actorRole) {
        Long bidId = JsonAuditPayload.longValue(payload, "bidId");
        return AuditDescriptor.success(
                "MARKETPLACE",
                action,
                "BID",
                bidId,
                actorAccountId,
                actorRole,
                JsonAuditPayload.details(
                        "bidId", bidId,
                        "listingId", JsonAuditPayload.longValue(payload, "listingId"),
                        "investorId", JsonAuditPayload.longValue(payload, "investorId"),
                        "reason", JsonAuditPayload.textValue(payload, "reason")
                )
        );
    }

    private AuditDescriptor agreementDescriptor(JsonNode payload, Long actorAccountId) {
        Long agreementId = JsonAuditPayload.longValue(payload, "agreementId");
        return AuditDescriptor.success(
                "MARKETPLACE",
                "AGREEMENT_CREATED",
                "AGREEMENT",
                agreementId,
                actorAccountId,
                "STARTUP",
                JsonAuditPayload.details(
                        "agreementId", agreementId,
                        "listingId", JsonAuditPayload.longValue(payload, "listingId"),
                        "bidId", JsonAuditPayload.longValue(payload, "bidId"),
                        "startupId", JsonAuditPayload.longValue(payload, "startupId"),
                        "investorId", JsonAuditPayload.longValue(payload, "investorId")
                )
        );
    }
}
