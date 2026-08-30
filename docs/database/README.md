# Database Guide

[Back to the documentation portal](../README.md)

Flyway owns schema creation and evolution. Hibernate validates the migrated
schema and never generates or updates it.

## Start Here

| Task | Reference |
|---|---|
| Understand how data moves across business areas | [Relational journey](relationship-journey.md) |
| Answer a specific relationship question | [Focused relationship views](views/README.md) |
| Check notation, guarantees, and intentional correlations | [Schema reference](reference/README.md) |
| Author, test, or recover a schema change | [Migration guide](migrations.md) |
| Inspect the executable baseline | [V1 baseline SQL](../../src/main/resources/db/migration/V1__baseline.sql) |

## Schema Areas

The focused views split the schema into readable contexts:

- account access and participant profiles;
- marketplace listings, bids, agreements, and acceptance terms;
- settlement, repayment, payment intents, attempts, and webhooks; and
- notification delivery, event outbox, and audit correlation.

Solid ER relationships represent database foreign keys. Event correlation
without a foreign key is identified explicitly so diagrams do not imply a
constraint that the schema does not contain.

The V1 baseline currently defines 35 tables and 46 foreign keys. Automated
regression tests derive the documentation inventory from Flyway and verify that
every table and relationship remains represented. The generated verification
artifact is intentionally kept out of reader navigation; use the focused views
or schema reference for human-readable answers.

`login_attempt` is shown as a standalone security log because the schema
deliberately defines no foreign key from it to `account` or `credential`.

## Ownership Rules

- Add the next unused version under `src/main/resources/db/migration`.
- Never edit, rename, reorder, or delete an applied migration.
- Keep `spring.flyway.baseline-on-migrate=false` and Flyway clean disabled.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Rehearse populated-database upgrades and recovery on a restored copy.

Read the [migration guide](migrations.md) before changing the schema.
