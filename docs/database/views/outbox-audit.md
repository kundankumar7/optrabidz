# Outbox and Audit

[Back to focused views](README.md)

<a href="../assets/outbox-audit-schema.svg"><img src="../assets/outbox-audit-schema.svg" alt="Outbox and audit relational schema"></a>

[High-resolution PNG fallback](../assets/outbox-audit-schema.png)

This slice shows `event_id` correlation. Dotted lines are not foreign keys; the
only solid FK is the nullable audit actor account.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `event_outbox` | correlation | `notification` | shared `event_id`; no FK exists; `event_outbox.event_id` is `UNIQUE`; `notification` has `UNIQUE (event_id, notification_name)` |
| `R2` | `event_outbox` | correlation | `audit_record` | shared nullable `event_id`; no FK exists; `event_outbox.event_id` is `UNIQUE`; `audit_record` has `UNIQUE (event_id, action)` |
| `R3` | `account` | `0..1 -> 0..N` | `audit_record` | `audit_record.actor_account_id` is nullable `FK`; `fk_audit_account`; `ON DELETE SET NULL` |
