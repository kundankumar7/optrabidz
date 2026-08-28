# Database Guide

[Back to the documentation portal](../README.md)

Flyway owns schema creation and evolution. Hibernate validates the migrated
schema and never generates or updates it.

## Start Here

| Task | Reference |
|---|---|
| Understand tables and relationships | [ER diagram index](er-diagram.md) |
| Author, test, or recover a schema change | [Migration guide](migrations.md) |
| Inspect the executable baseline | [V1 baseline SQL](../../src/main/resources/db/migration/V1__baseline.sql) |

## Schema Areas

The ER index splits the schema into readable contexts:

- account access and participant profiles;
- marketplace listings, bids, agreements, and acceptance terms;
- settlement, repayment, payment intents, attempts, and webhooks; and
- notification delivery, event outbox, and audit correlation.

Solid ER relationships represent database foreign keys. Event correlation
without a foreign key is identified explicitly so diagrams do not imply a
constraint that the schema does not contain.

The V1 baseline currently defines 35 tables. The published ER set visualizes
34; `login_attempt` remains schema-backed but is not yet represented in the
diagram set. Until the diagrams are remediated, use `V1__baseline.sql` as the
complete relationship reference.

## Ownership Rules

- Add the next unused version under `src/main/resources/db/migration`.
- Never edit, rename, reorder, or delete an applied migration.
- Keep `spring.flyway.baseline-on-migrate=false` and Flyway clean disabled.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Rehearse populated-database upgrades and recovery on a restored copy.

Read the [migration guide](migrations.md) before changing the schema.
