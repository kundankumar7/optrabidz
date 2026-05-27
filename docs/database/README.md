# Database Design Documents

This folder contains the database design artifacts for OptraBidz.

## Start Here

Open the ER diagram first:

[View the ER diagram](er-diagram.md)

Use the schema file when you need the exact PostgreSQL table, constraint, index,
enum, trigger, and seed-reference details:

[View the PostgreSQL schema](optrabidz-schema.sql)

## Files

| File | Purpose |
|---|---|
| `er-diagram.md` | Reviewer-friendly ER diagram split by domain for readability |
| `optrabidz-schema.sql` | PostgreSQL schema reference for tables, constraints, indexes, enum types, triggers, and small reference seed data |

## How The Application Uses The Database

For local development, the application can run with an empty PostgreSQL database
named `optrabidz`. The development profile uses Hibernate/JPA to create or
update the database structure from the entity mappings.

The SQL file in this folder is included for review, documentation, and optional
manual initialization.

The ER diagram is based on the schema relationships in
`optrabidz-schema.sql`. Solid lines represent foreign keys. Any non-FK event
correlation is marked separately so the diagram does not imply database
relationships that do not exist.

## Optional Manual Initialization

If you want to initialize the schema manually before starting the application:

```powershell
psql -U postgres -d optrabidz -f docs/database/optrabidz-schema.sql
```

The project does not require a database backup file to run. Backup files can
contain local test data and should not be published as project documentation.
