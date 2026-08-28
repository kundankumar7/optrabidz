# Operations Guide

[Back to the documentation portal](../README.md)

## Runtime Profiles

| Profile | Purpose |
|---|---|
| default | Fail-closed baseline; API docs and local financial adapters are disabled |
| `dev` | Local development with Swagger UI and sandbox integrations |
| `prod` | Production datasource and authenticated API-document access settings |

Production requires `OPTRABIDZ_DATASOURCE_URL`,
`OPTRABIDZ_DATASOURCE_USERNAME`, and `OPTRABIDZ_DATASOURCE_PASSWORD`.
`OPTRABIDZ_API_DOCS_ENABLED` may explicitly enable the OpenAPI document while
Swagger UI remains disabled. This guide names configuration keys only; secret
values belong in the deployment platform's secret store.

## Database Startup

Flyway validates and applies pending versioned migrations before Hibernate
validates entity mappings. Startup must fail when migration history, checksums,
or entity mappings do not agree with the database.

See [database migrations](../database/migrations.md) before deploying a schema
change or recovering a failed migration.

## Scheduled Workers

The application runs three scheduled responsibilities:

| Worker | Configuration prefix | Responsibility |
|---|---|---|
| Outbox dispatcher | `optrabidz.outbox.dispatcher` | Locks committed events and invokes audit and notification processors |
| Notification dispatcher | `optrabidz.notification.dispatcher` | Claims pending deliveries, records attempts, and applies retry limits |
| Lifecycle expiry scheduler | `optrabidz.governance.lifecycle.scheduler` | Expires eligible marketplace lifecycle records in bounded batches |

Each worker has an `enabled` switch and bounded delay or batch settings. Review
database locking, retry behavior, idempotency, and multi-instance execution
before changing those values.

## Integration Configuration

Local and sandbox financial providers are disabled by default and in the
production profile. Webhook secrets must be provided through environment
configuration. Notification channels use application configuration and the
current local delivery adapters; external broker or delivery-provider
infrastructure is not implemented.

## Operational Checks

Before a release:

1. run unit and PostgreSQL integration tests;
2. validate Flyway history and rehearse any populated-database migration;
3. confirm local and sandbox providers are disabled in the target profile;
4. verify required secrets are supplied without logging their values;
5. inspect outbox and notification backlog, retry, and failed-delivery counts;
6. confirm request IDs connect API failures to server logs and audit records;
7. record a rollback or forward-recovery checkpoint.

Never publish credentials, webhook secrets, access tokens, database dumps, or
machine-specific filesystem paths in repository documentation or tickets.
