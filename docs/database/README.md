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

The V1 baseline currently defines 35 tables, and all 35 appear in the published
ER source. `login_attempt` is shown as a standalone security log because the
schema deliberately defines no foreign key from it to `account` or
`credential`. An automated coverage check keeps the Flyway table set and ER
entity set aligned.

## Ownership Rules

- Add the next unused version under `src/main/resources/db/migration`.
- Never edit, rename, reorder, or delete an applied migration.
- Keep `spring.flyway.baseline-on-migrate=false` and Flyway clean disabled.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Rehearse populated-database upgrades and recovery on a restored copy.

Read the [migration guide](migrations.md) before changing the schema.
