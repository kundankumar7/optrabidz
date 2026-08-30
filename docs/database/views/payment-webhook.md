# Payment Webhooks

[Back to focused views](README.md)

<a href="../assets/payment-webhook-schema.svg"><img src="../assets/payment-webhook-schema.svg" alt="Payment webhook relational schema"></a>

[High-resolution PNG fallback](../assets/payment-webhook-schema.png)

This slice shows provider webhook idempotency and the optional references a
webhook event may carry back to payment records.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `payment_provider` | `1 -> 0..N` | `payment_webhook_event` | `payment_webhook_event.provider_code` is `FK`, `NOT NULL`; `fk_payment_webhook_provider`; `ON DELETE RESTRICT` |
| `R2` | `payment_intent` | `0..1 -> 0..N` | `payment_webhook_event` | `payment_webhook_event.payment_intent_id` is nullable `FK`; `fk_payment_webhook_intent`; `ON DELETE RESTRICT` |
| `R3` | `payment_attempt` | `0..1 -> 0..N` | `payment_webhook_event` | `payment_webhook_event.payment_attempt_id` is nullable `FK`; `fk_payment_webhook_attempt`; `ON DELETE RESTRICT` |
