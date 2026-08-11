# Database Design Documents

This folder contains the database design artifacts for OptraBidz.

## Start Here

Open the ER diagrams index first:

[View the ER diagrams](er-diagram.md)

Use the Flyway V1 migration when you need the exact PostgreSQL table,
constraint, index, enum, trigger, and seed-reference details:

[View the executable V1 baseline](../../src/main/resources/db/migration/V1__baseline.sql)

## Current ER Diagram Set

The ER diagrams are split into the same schema-backed slices listed in
`er-diagram.md`:

| Area | Diagrams |
|---|---|
| Identity and Access | [Account Access and Security Context](er-diagram.md#account-access-and-security-context); [Participant Profile Context](er-diagram.md#participant-profile-context) |
| Marketplace | [Marketplace Listing and Bidding Context](er-diagram.md#marketplace-listing-and-bidding-context); [Agreement Acceptance and Debt Terms Context](er-diagram.md#agreement-acceptance-and-debt-terms-context) |
| Finance | [Settlement Context](er-diagram.md#settlement-context); [Repayment Schedule Context](er-diagram.md#repayment-schedule-context) |
| Payments | [Payment Intent Context](er-diagram.md#payment-intent-context); [Payment Attempt and Provider Context](er-diagram.md#payment-attempt-and-provider-context); [Payment Webhook Context](er-diagram.md#payment-webhook-context) |
| Notifications, Outbox, and Audit | [Notification Delivery and Subscription Context](er-diagram.md#notification-delivery-and-subscription-context); [Outbox and Audit Correlation Context](er-diagram.md#outbox-and-audit-correlation-context) |

## Files

| File | Purpose |
|---|---|
| `er-diagram.md` | Reviewer-friendly ER diagrams split by schema context for readability |
| `er-diagram-source.md` | Editable Mermaid source for the rendered ER diagrams |
| `../../src/main/resources/db/migration/V1__baseline.sql` | Executable Flyway V1 baseline for tables, constraints, indexes, enum types, triggers, and small reference seed data |

## How The Application Uses The Database

The reviewed schema is now Flyway migration `V1__baseline.sql`. A fresh database
must reach version 1 by running Flyway; Hibernate entity mappings are not the
schema source of truth.

Environment startup ordering and the switch to Hibernate validation are handled
separately so this migration change remains reviewable.

The ER diagrams are based on the schema relationships in
`V1__baseline.sql`. Solid lines represent foreign keys. Any non-FK event
correlation is marked separately so the diagrams do not imply database
relationships that do not exist.

## Migration Ownership

Apply this file through Flyway so the schema history table records version 1
and its checksum. Do not run the SQL directly with `psql`, and do not edit a
released migration; later schema changes must use a new versioned migration.
