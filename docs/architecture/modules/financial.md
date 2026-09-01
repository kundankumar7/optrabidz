# Financial Module

[Back to the module catalogue](README.md)

Capability: [Finance and payments](../capabilities/finance-payments.md)

## Purpose

Coordinate settlements, repayments, installments, payment intents and attempts,
provider strategies, signed callbacks, and replay-safe payment state changes.

## Entry points

`FinancialController` owns authenticated finance queries and commands.
`LocalPaymentSimulationController` is a local-adapter surface.
`PaymentProviderWebhookController` and `PaymentWebhookHttpIngress` form the
provider callback boundary.

## Application and domain

`FinancialService` coordinates settlement, repayment, intent, and attempt
lifecycles. Webhook ingress, signature registries, strict parsing, replay
services, payment-method strategies, and state-rich domain models protect
callback processing and state transitions.

## Persistence

JPA entities and adapters persist settlements, repayments, installments,
payment intents, attempts, providers, and provider methods.
`PostgresPaymentWebhookReplayStore` owns callback replay evidence.

## Events

Settlement confirmation and installment success/failure events feed outbox
audit and notification processing. Replay events record callback handling.

## Dependencies

Direct imports reach `audit`, `common`, `governance`, `identity`, `marketplace`,
`participation`, and `security`; adapters bridge agreement and lifecycle ports.

## Security and errors

User operations require authenticated ownership. Webhooks enforce provider
configuration, payload size, HMAC signature, timestamp, strict event parsing,
provider isolation, replay collision handling, and neutral disclosure.

## Verification

Thirty module tests cover services, states, filters, provider isolation,
signatures, replay, controllers, and PostgreSQL-backed integration behavior.

## Known gaps

Only local and sandbox payment strategies are implemented. Real-money provider
SDKs, settlement reconciliation, refunds, disputes, and production credential
operations require dedicated future work.
