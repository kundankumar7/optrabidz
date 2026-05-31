# Database Design Documents

This folder contains the database design artifacts for OptraBidz.

## Start Here

Open the ER diagrams index first:

[View the ER diagrams](er-diagram.md)

Use the schema file when you need the exact PostgreSQL table, constraint, index,
enum, trigger, and seed-reference details:

[View the PostgreSQL schema](optrabidz-schema.sql)

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
| `optrabidz-schema.sql` | PostgreSQL schema reference for tables, constraints, indexes, enum types, triggers, and small reference seed data |

## How The Application Uses The Database

For local development, the application can run with an empty PostgreSQL database
named `optrabidz`. The development profile uses Hibernate/JPA to create or
update the database structure from the entity mappings.

The SQL file in this folder is included for review, documentation, and optional
manual initialization.

The ER diagrams are based on the schema relationships in
`optrabidz-schema.sql`. Solid lines represent foreign keys. Any non-FK event
correlation is marked separately so the diagrams do not imply database
relationships that do not exist.

## Optional Manual Initialization

If you want to initialize the schema manually before starting the application:

```powershell
psql -U postgres -d optrabidz -f docs/database/optrabidz-schema.sql
```

The project does not require a database backup file to run. Backup files can
contain local test data and should not be published as project documentation.
