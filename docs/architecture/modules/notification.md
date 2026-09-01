# Notification Module

[Back to the module catalogue](README.md)

Capability: [Platform support](../capabilities/platform-support.md)

## Purpose

Create user notifications from committed events, store per-recipient state,
manage channel subscriptions, and dispatch retryable channel deliveries.

## Entry points

`NotificationController` exposes the current actor's feed, summary, read/delete
actions, and subscription management. `NotificationEventHandler` consumes
outbox work and `NotificationDeliveryDispatcher` runs scheduled delivery.

## Application and domain

`NotificationService` owns feed and subscription use cases. Rule registries map
account, profile, marketplace, governance, and finance events to notification
plans and recipients. Channel selection and proxy components isolate delivery
strategies.

## Persistence

Five JPA entities and repositories store notification content, recipients,
subscriptions, deliveries, and delivery attempts.

## Events

The module consumes outbox events through `NotificationEventHandler`; it does
not publish a module-local domain event.

## Dependencies

Production source directly imports `common` outbox/error contracts and the
authenticated actor abstraction from `security`.

## Security and errors

Feed, mutation, and subscription operations are scoped to the authenticated
recipient. Missing notification and subscription errors do not expose another
actor's data.

## Verification

Five module tests cover controller, service, event-rule, persistence, and
delivery-dispatch behavior.

## Known gaps

Email and push are sandbox strategies. Kafka, external provider adapters,
provider webhooks, templates, and production delivery observability are not
implemented.
