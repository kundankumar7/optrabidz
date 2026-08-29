# Database Guide

[Back to the documentation portal](../README.md)

Flyway owns schema creation and evolution. Hibernate validates the migrated
schema and never generates or updates it.

## Start Here

| Task | Reference |
|---|---|
| Follow the data model from account to audit | [Relational journey](relationship-journey.md) |
| Understand tables and relationships | [ER diagram index](er-diagram.md) |
| Inspect the machine-verified relationship inventory | [Schema manifest](schema-manifest.json) |
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

The V1 baseline currently defines 35 tables and 46 foreign keys. The schema
manifest records every FK's child and parent columns, nullability, and delete
behavior together with 25 unique constraints, 57 checks, 19 partial indexes,
12 triggers, and explicitly identified non-FK correlations. Its regression
test derives those facts from Flyway, while the diagram coverage test keeps all
35 tables represented in the published ER source.

`login_attempt` is shown as a standalone security log because the schema
deliberately defines no foreign key from it to `account` or `credential`.

## Ownership Rules

- Add the next unused version under `src/main/resources/db/migration`.
- Never edit, rename, reorder, or delete an applied migration.
- Keep `spring.flyway.baseline-on-migrate=false` and Flyway clean disabled.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Rehearse populated-database upgrades and recovery on a restored copy.

Read the [migration guide](migrations.md) before changing the schema.
