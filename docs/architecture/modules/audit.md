# Audit Module

[Back to the module catalogue](README.md)

## Purpose

Persist searchable business and security audit records after committed domain
work while keeping public responses separate from internal evidence.

## Entry points

`AdminAuditController` exposes the administrator audit query. Outbox work enters
through `AuditEventHandler`; security-sensitive code can call
`SecurityAuditService` directly at its application boundary.

## Application and domain

`AuditService` queries records. `AuditRecordFactory`, `AuditPolicyRegistry`, and
the account, classification, governance, marketplace, participation, finance,
and default policies translate event metadata into audit descriptors and
outcomes.

## Persistence

`AuditRecord` is the JPA entity and `JpaAuditRecordRepository` owns database
access. Audit data is written after outbox dispatch, not inside controllers.

## Events

`AuditEventHandler` implements `OutboxEventProcessor` and consumes supported
committed event types.

## Dependencies

Production source directly imports `common` outbox and error/event contracts.

## Security and errors

The HTTP query is administrator-restricted. Stored evidence may contain
diagnostic context and must not be copied into Problem Details responses.

## Verification

Four module tests cover the controller, service, event handler, and security
audit behavior; the shared integration suite covers outbox dispatch.

## Known gaps

The audit store is database-backed and not tamper-evident or exported to an
external security information and event management system.
