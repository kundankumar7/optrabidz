# Payment Intent

[Back to focused views](README.md)

<a href="../assets/payment-intent-schema.svg"><img src="../assets/payment-intent-schema.svg" alt="Payment intent relational schema"></a>

[High-resolution PNG fallback](../assets/payment-intent-schema.png)

This slice shows how a `payment_intent` is sourced and which accounts
participate. Provider, attempt, and webhook relationships are separate views.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `settlement` | `0..1 -> 0..N` | `payment_intent` | `payment_intent.settlement_id` is nullable `FK`; `fk_payment_intent_settlement`; `ON DELETE RESTRICT` |
| `R2` | `repayment_installment` | `0..1 -> 0..N` | `payment_intent` | `payment_intent.repayment_installment_id` is nullable `FK`; `fk_payment_intent_repayment_installment`; `ON DELETE RESTRICT` |
| `R3` | `account` | `1 -> 0..N` | `payment_intent` | `payment_intent.payer_account_id` is `FK`, `NOT NULL`; `fk_payment_intent_payer_account`; `ON DELETE RESTRICT` |
| `R4` | `account` | `1 -> 0..N` | `payment_intent` | `payment_intent.payee_account_id` is `FK`, `NOT NULL`; `fk_payment_intent_payee_account`; `ON DELETE RESTRICT` |
