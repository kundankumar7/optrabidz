# Payment Processing

[Back to focused views](README.md)

<a href="../assets/payment-processing-schema.svg"><img src="../assets/payment-processing-schema.svg" alt="Payment processing relational schema"></a>

[High-resolution PNG fallback](../assets/payment-processing-schema.png)

This slice shows payment attempts and provider configuration. Webhook
relationships are intentionally separate because their intent and attempt
references are optional.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `payment_intent` | `1 -> 0..N` | `payment_attempt` | `payment_attempt.payment_intent_id` is `FK`, `NOT NULL`; `fk_payment_attempt_intent`; `ON DELETE RESTRICT` |
| `R2` | `payment_provider` | `1 -> 0..N` | `payment_attempt` | `payment_attempt.provider_code` is `FK`, `NOT NULL`; `fk_payment_attempt_provider`; `ON DELETE RESTRICT` |
| `R3` | `payment_provider` | `1 -> 0..N` | `payment_provider_method` | `payment_provider_method.provider_code` is `FK`, part of composite `PK`; `fk_payment_provider_method_provider`; `ON DELETE RESTRICT` |
