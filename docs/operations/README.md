# Operations Guide

[Back to the documentation portal](../README.md)

## Runtime Profiles

| Profile | Purpose |
|---|---|
| default | Fail-closed baseline; API docs and local financial adapters are disabled |
| `dev` | Local development with Swagger UI and optional, disabled-by-default integrations |
| `test` | Automated tests with isolated fixtures and Testcontainers |
| `prod` | Production datasource and authenticated API-document access settings |

The application does not activate a profile automatically. Local development
copies `.env.example` to ignored `.env`; only `application-dev.properties`
imports that file. CI uses test configuration. Production receives values from
its deployment environment, mounted configuration, or secret store and never
loads `.env`.

Production requires `OPTRABIDZ_DATASOURCE_URL`,
`OPTRABIDZ_DATASOURCE_USERNAME`, and `OPTRABIDZ_DATASOURCE_PASSWORD`.
`OPTRABIDZ_API_DOCS_ENABLED` may explicitly enable the OpenAPI document while
Swagger UI remains disabled. This guide names configuration keys only; do not
copy credential values into commands, logs, shared messages, or documentation.

## Local Runtime Lifecycle

Run these commands from the repository root. The Compose project contains only
the local PostgreSQL service. Spring Boot runs separately through Maven or
IntelliJ and should be stopped before PostgreSQL maintenance.

Inspect the service without displaying its environment:

```powershell
docker compose ps
docker compose logs --tail 100 postgres
```

Stop PostgreSQL while preserving both its container and named data volume:

```powershell
docker compose stop postgres
```

Resume that stopped container with its existing data:

```powershell
docker compose start postgres
docker compose ps
```

To remove the project container and network while preserving the named data
volume, use:

```powershell
docker compose down
```

Resume after `down` by recreating the service against the preserved volume:

```powershell
docker compose up -d postgres
docker compose ps
```

Do not add the volume-removal option to routine stop or shutdown commands.
Permanent reset is a separate operation owned by the
[database migration guide](../database/migrations.md#resetting-the-disposable-local-database).

The Compose project name is fixed as `optrabidz`. Different checkouts on the
same computer therefore refer to the same local project and
`optrabidz_postgres-data` volume. Manage only one active local Compose
database at a time.

## Local Runtime Troubleshooting

### Port 5432 is occupied

Identify the listener before taking action:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue
docker ps --filter publish=5432
```

Do not stop, remove, or reconfigure an unrelated process or container. Either
stop the known local service through its normal owner or use compatible Native
PostgreSQL 16 and update all three datasource values consistently.

### PostgreSQL is not healthy

Run `docker compose ps` and `docker compose logs --tail 100 postgres`. Check
that Docker Engine is running and that `.env` contains a non-blank
`OPTRABIDZ_DATASOURCE_PASSWORD`. Do not place the value in diagnostic output.

### The datasource password no longer matches

The PostgreSQL image uses its configured password only when
`optrabidz_postgres-data` is first initialized. Editing `.env` later does not
change the existing database role. Restore the original local value, change
the role password through PostgreSQL administration, or—only when every record
is disposable—use the reviewed reset procedure in the
[database migration guide](../database/migrations.md#resetting-the-disposable-local-database).

### Flyway or Hibernate blocks startup

Read the first migration or schema-validation error and follow the
[database migration guide](../database/migrations.md). Never reset a database
whose data must be preserved merely to make startup succeed.

### Administrator bootstrap blocks startup

Confirm that bootstrap and recovery are not both enabled. When bootstrap is
enabled, all four administrator identity fields must be present and valid.
After successful provisioning, stop the application, set
`OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=false`, blank every one-time bootstrap
field, and restart.

### Bootstrap was left enabled

Stop the application and remove the retained one-time values immediately. An
existing administrator remains stored in PostgreSQL; disabling bootstrap does
not delete or disable that account. Restart and verify readiness, login, and
`GET /api/v1/me`.

## Privileged Operations

Bootstrap and recovery are mutually exclusive. Enabling both prevents startup.
Missing or invalid enabled-feature configuration also prevents readiness, and
the diagnostic names the feature without reproducing the submitted value.

### First Administrator Provisioning

Use this only when no active administrator exists:

1. Stop the application and restrict access to the local or deployment
   configuration surface.
2. Supply `OPTRABIDZ_ADMIN_BOOTSTRAP_EMAIL`,
   `OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD`,
   `OPTRABIDZ_ADMIN_BOOTSTRAP_DISPLAY_NAME`, and
   `OPTRABIDZ_ADMIN_BOOTSTRAP_ORGANIZATION` externally.
3. Set `OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=true` and keep
   `OPTRABIDZ_ADMIN_RECOVERY_ENABLED=false`.
4. Start once and verify the new account through the normal login path. If an
   active administrator already exists, the runner records that bootstrap was
   skipped.
5. Stop the application, set the bootstrap switch to `false`, and remove the
   bootstrap password and other one-time fields from the environment or
   ignored local file.
6. Restart normally and confirm the application is healthy without the
   bootstrap runner.

Retaining the enable flag or password after provisioning is an operational
defect even though the service will not create a second active administrator.

### Administrator Recovery

Recovery transfers authority, disables the previous administrator credential,
and deactivates that account. Use it only in an authorized maintenance window:

1. Confirm the target administrator details, recovery authorization, database
   backup or rollback point, and audit owner.
2. Generate a new 32–512 byte token and supply it externally as
   `OPTRABIDZ_ADMIN_RECOVERY_TOKEN`.
3. Set `OPTRABIDZ_ADMIN_RECOVERY_ENABLED=true` and keep
   `OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=false`, then restart.
4. Send the approved transfer request to
   `POST /api/v1/admin/recovery/transfer` with the token in the
   `X-ADMIN-RECOVERY-TOKEN` header. Use a client that does not retain the token
   or the new administrator password in shell history or shared logs.
5. Verify the new administrator login, the previous account deactivation, and
   the transfer audit evidence.
6. Stop the application, disable recovery, remove the token, and restart.
7. Confirm the recovery controller is no longer registered and record only
   non-secret completion evidence.

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

Local and sandbox financial providers are disabled by default. They can start
only when the active profile set contains exclusively `dev` and/or `test`; no
profile, `prod`, a mixed `prod,dev` set, or any other profile fails startup if
either adapter is requested.

To test a payment flow locally:

1. Start with the `dev` profile.
2. Enable only `OPTRABIDZ_LOCAL_PROVIDER_ENABLED` or
   `OPTRABIDZ_SANDBOX_PROVIDERS_ENABLED`, according to the flow under test.
3. If a UPI or card webhook is required, enable only its provider switch and
   supply a 32–512 byte test secret through ignored `.env` or the process
   environment.
4. Restart, execute the focused scenario, and retain only non-secret evidence.
5. Disable the adapter and webhook, remove the test secret, and restart.

Disabled webhook providers require no secret. Enabled providers fail before
readiness when secret material is missing or invalid. When profiles are mixed,
production restrictions take precedence.

Notification channels use application configuration and the current local
delivery adapters; external broker or delivery-provider infrastructure is not
implemented.

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
machine-specific filesystem paths in repository documentation or shared
messages.
