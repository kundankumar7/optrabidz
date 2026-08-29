# Event Delivery Flow

[Back to architecture](../README.md)

## Transaction boundary

1. An application service accepts a state transition.
2. The service persists business state and publishes a domain event.
3. `OutboxWriter` stores the serialized event in the same database transaction.
4. The HTTP request may return after the transaction commits; it does not wait
   for audit or notification channel delivery.

## Post-commit processing

1. `OutboxDispatcher` claims a bounded batch of committed outbox rows.
2. Registered processors select work by event type.
3. `AuditEventHandler` resolves an audit policy and persists an audit record.
4. `NotificationEventHandler` resolves notification rules, recipients, and
   subscribed channels, then persists notification and delivery records.
5. `NotificationDeliveryDispatcher` claims pending channel deliveries and
   records attempts, success, retry, or terminal failure.

The outbox decouples business commit success from supporting side effects. It
does not provide exactly-once execution by itself; processors and state changes
must remain replay-safe and observable.

## Implemented delivery channels

- In-app notification persistence is implemented.
- Email and push are sandbox strategies.
- Kafka and external delivery providers are not implemented.

Operational configuration is documented in the
[operations guide](../../operations/README.md).
