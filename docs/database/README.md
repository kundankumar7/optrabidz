# Database Design Documents

This folder contains the database design artifacts for OptraBidz.

[Back to the documentation portal](../README.md)

## Start Here

Open the ER diagrams index first:

[View the ER diagrams](er-diagram.md)

Read the migration guide before authoring or applying a schema change:

[View the database migration guide](migrations.md)

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
| `migrations.md` | Migration authoring, environment upgrade, and recovery policy |
| `../../src/main/resources/db/migration/V1__baseline.sql` | Executable Flyway V1 baseline for tables, constraints, indexes, enum types, triggers, and small reference seed data |

## Work-item History

These records explain the decisions and verification behind the current
database guidance. They are historical delivery records, not substitutes for
the current references above.

| Jira | Decision record | Delivery plan |
|---|---|---|
| [KAN-12](https://0707manna0895.atlassian.net/browse/KAN-12) | [Migration policy](work-items/KAN-12-migration-policy/design.md) | [Implementation plan](work-items/KAN-12-migration-policy/implementation-plan.md) |
| [KAN-14](https://0707manna0895.atlassian.net/browse/KAN-14) | [Database foundation release](work-items/KAN-14-database-foundation-release/design.md) | [Implementation plan](work-items/KAN-14-database-foundation-release/implementation-plan.md) |

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

Flyway owns schema creation and upgrades; Hibernate only validates the migrated
schema. Follow the [database migration guide](migrations.md) for authoring,
local reset, populated-database upgrade, and recovery rules.
