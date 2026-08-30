# Focused Relationship Views

[Back to the database guide](../README.md)

Choose the smallest view that answers the question. Each page owns one diagram
and the exact Flyway-backed relationship table for that slice.

## Choose a relationship view

| Question | View |
|---|---|
| How are accounts, credentials, sessions, roles, and administrators related? | [Identity and access](identity-access.md) |
| How are startup and investor details attached? | [Participant profiles](participant-profile.md) |
| How do listings and bids connect? | [Marketplace and bidding](marketplace-bidding.md) |
| What becomes durable when a bid is accepted? | [Agreement acceptance](agreement-acceptance.md) |
| How does an accepted agreement become a settlement? | [Settlement](settlement.md) |
| How is investor repayment scheduled? | [Repayment schedule](repayment-schedule.md) |
| How is a payment purpose tied to payer and payee accounts? | [Payment intent](payment-intent.md) |
| How do attempts select a configured provider? | [Payment processing](payment-processing.md) |
| How are provider callbacks deduplicated and linked? | [Payment webhooks](payment-webhook.md) |
| How are notification recipients, channels, and attempts tracked? | [Notification delivery](notification-delivery.md) |
| Which event links are correlations rather than foreign keys? | [Outbox and audit](outbox-audit.md) |

For end-to-end orientation, start with the [relational journey](../relationship-journey.md).
For notation and cross-cutting guarantees, use the [schema reference](../reference/README.md).
