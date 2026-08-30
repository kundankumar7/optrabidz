# Schema Reference

[Back to the database guide](../README.md)

This page records notation and cross-cutting facts that apply to every focused
relationship view. Flyway remains the executable source of truth.

## Relationship notation

| Marker | Meaning |
|---|---|
| `PK` | Primary key |
| `FK` | Foreign key |
| `UK` | Unique key |
| `1` | Exactly one row on that side is required by the FK |
| `0..1` | Zero or one row on that side is allowed by the schema |
| `0..N` | Zero or many rows on that side are allowed by the schema |
| Solid line | Database foreign-key relationship |
| Dotted line | Application correlation, not a database foreign key |

## Verified baseline

The V1 baseline defines 35 tables and 46 foreign keys. Verification also tracks
25 unique constraints, 57 checks, 19 partial indexes, and 12 triggers. Tests
derive these facts directly from Flyway and verify every published relationship.

Material guarantees include immutable login-attempt and audit records, one
active administrator, at most one accepted bid per listing, consistent agreement
participants, exclusive payment purposes, provider webhook deduplication, and
bounded notification-delivery retries.

## Intentional non-FK correlations

These links are maintained by application behavior or shared identifiers. They
are listed separately so they cannot be mistaken for database-enforced foreign
keys.

| ID | From | To | Basis |
|---|---|---|---|
| `outbox-notification-event` | `event_outbox.event_id` | `notification.event_id` | Shared event identifier; `notification` has `UNIQUE (event_id, notification_name)`, but no foreign key. |
| `outbox-audit-event` | `event_outbox.event_id` | `audit_record.event_id` | Shared nullable event identifier; `audit_record` has `UNIQUE (event_id, action)`, but no foreign key. |
| `payment-attempt-method` | `payment_attempt.(provider_code, method_type)` | `payment_provider_method.(provider_code, method_type)` | Application provider-method lookup; the provider-method primary key also includes `currency_code`, so no matching foreign key exists. |
| `settlement-confirmed-intent` | `settlement.confirmed_payment_intent_id` | `payment_intent.payment_intent_id` | Application-maintained confirmation reference; the schema declares no foreign key. |
| `installment-confirmed-intent` | `repayment_installment.confirmed_payment_intent_id` | `payment_intent.payment_intent_id` | Application-maintained confirmation reference; the schema declares no foreign key. |
| `login-attempt-email` | `login_attempt.email` | `credential.email` | Submitted-email security-log correlation only; immutable login attempts deliberately have no account or credential foreign key. |

## Verification boundary

`schema-manifest.json` is a machine-consumed regression artifact, not a reader
entry point. `DatabaseSchemaManifestTest` derives it from Flyway;
`DatabaseRelationshipDocumentationTest` compares it with the focused pages.
