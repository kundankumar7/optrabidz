# Getting Started

[Back to the documentation portal](../README.md)

This guide takes a new developer from a fresh clone to a verified local
administrator session. It is strictly a local-development workflow. Production
uses an immutable application image, externally supplied secrets, controlled
migrations, and separately reviewed operational procedures.

Commands below use Windows PowerShell and must be run from the repository root.

## 1. Check the prerequisites

Install:

- Temurin or another compatible Java 21 distribution;
- Git; and
- either Docker Desktop with Docker Engine running, or Native PostgreSQL 16.

The repository includes the Maven wrapper. Confirm the tools used by your
chosen path:

```powershell
java --version
git --version
docker version
```

The Docker command is required only for the recommended Compose path.

## 2. Clone and enter the repository

```powershell
git clone https://github.com/kundankumar7/optrabidz.git
Set-Location optrabidz
```

If the repository is already cloned, open a PowerShell terminal at its root.

## 3. Create local configuration

Copy the tracked template to the ignored local file:

```powershell
Copy-Item .env.example .env
```

The `dev` profile imports this file for the host-run Spring Boot application.
Docker Compose also reads it from the repository root. Production never loads
this file.

**Password checkpoint 1:** open `.env` locally now and enter a disposable
PostgreSQL password after `OPTRABIDZ_DATASOURCE_PASSWORD=`. Do not paste that
password into shared messages, documentation, screenshots, commits, or command
arguments. Use a long random value made from letters, digits, hyphens, and
underscores so Docker Compose and Spring Boot interpret it identically. Avoid
quotes, whitespace, dollar signs, number signs, and backslashes in this shared
local file.
Keep these matching datasource values:

```properties
OPTRABIDZ_DATASOURCE_URL=jdbc:postgresql://localhost:5432/optrabidz
OPTRABIDZ_DATASOURCE_USERNAME=postgres
OPTRABIDZ_DATASOURCE_PASSWORD=
```

The password line above is intentionally blank in this guide. Enter the value
only in your ignored `.env` file. Leave bootstrap, recovery, local payment,
sandbox payment, and webhook switches set to `false`; leave their secret fields
blank.

PostgreSQL applies its initial password only when the Compose data volume is
first created. Changing `.env` later does not change the password inside an
existing database. If they no longer match, follow the password-mismatch
guidance in the [operations guide](../operations/README.md).

## 4. Start PostgreSQL with Docker Compose

Docker Compose runs PostgreSQL 16 only. Spring Boot continues to run directly
on your computer through Maven or IntelliJ, preserving the normal edit and
debugging loop.

Validate the configuration and start the database:

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps
```

Wait until `postgres` reports `healthy`. If port 5432 is already occupied, do
not stop or delete the existing service until you identify it. Follow
[local troubleshooting](../operations/README.md#local-runtime-troubleshooting).

The Compose project has the fixed local name `optrabidz`. Use one active
OptraBidz Compose database at a time; another checkout refers to the same local
project and volume.

## 5. Start the application

### Maven

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### IntelliJ IDEA

1. Open `OptrabidzApplication` and create a Spring Boot run configuration.
2. Keep the working directory set to the repository root so `.env` can be
   found.
3. Set the active profile to `dev`, or add
   `-Dspring.profiles.active=dev` to the VM options.
4. Run the configuration.

During startup, confirm that Flyway validates and applies migrations and that
Hibernate completes schema validation. Then check readiness:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

Continue only when the response status is `UP`.

## 6. Open the local API documentation

Open `http://localhost:8080/swagger-ui.html`. Swagger UI is enabled by the
`dev` profile and remains disabled by default outside local development.

Startup and investor self-registration are normal public flows. Administrator
self-registration is deliberately unavailable; the first administrator uses
the guarded one-startup procedure below.

## 7. Provision the first local administrator

Use this only when the local database has no active administrator.

1. Stop the application but leave PostgreSQL running.
2. Open the ignored `.env` file locally.
3. Set `OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=true` and keep
   `OPTRABIDZ_ADMIN_RECOVERY_ENABLED=false`.
4. Enter a local administrator email, display name, and organization in the
   corresponding bootstrap fields.
5. **Password checkpoint 2:** enter a separate locally chosen administrator
   password after `OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD=` now. Do not share or
   record it outside `.env`. It must contain at least one letter and one digit;
   prefer a long random value using the same parser-safe characters described
   at password checkpoint 1.
6. Start the application once and wait for readiness to return `UP`.

The required local fields are:

```properties
OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=true
OPTRABIDZ_ADMIN_BOOTSTRAP_EMAIL=
OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD=
OPTRABIDZ_ADMIN_BOOTSTRAP_DISPLAY_NAME=
OPTRABIDZ_ADMIN_BOOTSTRAP_ORGANIZATION=
```

The blank fields are reminders, not values to paste over completed local
configuration. The runner creates one administrator only when no active
administrator exists.

## 8. Verify login and session restoration

Use the same local Swagger UI tab so the browser retains the session cookie:

1. Execute `POST /api/v1/auth/login` with the local administrator email and
   password. Expect HTTP 200 and role `ADMIN`.
2. In the same browser session, execute `GET /api/v1/me`. Expect
   HTTP 200, role `ADMIN`, account state `ACTIVE`, profile status `COMPLETE`,
   and `actorExists` set to `true`.
3. Clear the password from the visible Swagger request field. Do not capture or
   share a screenshot containing the request.

## 9. Disable bootstrap and remove one-time values

The administrator account is now stored in PostgreSQL and does not depend on
the bootstrap values remaining present.

1. Stop the application.
2. Change the ignored `.env` file to the following disabled and blank state:

   ```properties
   OPTRABIDZ_ADMIN_BOOTSTRAP_ENABLED=false
   OPTRABIDZ_ADMIN_BOOTSTRAP_EMAIL=
   OPTRABIDZ_ADMIN_BOOTSTRAP_PASSWORD=
   OPTRABIDZ_ADMIN_BOOTSTRAP_DISPLAY_NAME=
   OPTRABIDZ_ADMIN_BOOTSTRAP_ORGANIZATION=
   ```

3. Restart the application normally.
4. Confirm readiness is `UP`.
5. Log in again and repeat `GET /api/v1/me` to prove the stored administrator
   works while bootstrap is disabled and its one-time values are absent.

Leaving the enable switch or any one-time bootstrap value behind is a local
configuration defect.

## 10. Stop and resume safely

Stop the application with `Ctrl+C`. To stop PostgreSQL without deleting local
data, follow the [local runtime lifecycle](../operations/README.md#local-runtime-lifecycle).
The destructive reset procedure is deliberately kept out of this happy path.

## Use Native PostgreSQL 16 instead

Docker is optional when compatible PostgreSQL 16 is already installed. Skip
the Compose commands and ensure that the native server:

- listens at the host and port in `OPTRABIDZ_DATASOURCE_URL`;
- contains a database named `optrabidz`;
- accepts `OPTRABIDZ_DATASOURCE_USERNAME` and
  `OPTRABIDZ_DATASOURCE_PASSWORD`; and
- gives the local user the schema privileges Flyway needs for migrations.

Start the native server using its own service manager, then continue at
[Start the application](#5-start-the-application). Native installation,
startup, backup, and removal remain the developer's responsibility. Never use
the disposable Compose reset procedure for a native or shared database.

## Troubleshooting and verification

The [operations guide](../operations/README.md#local-runtime-troubleshooting)
covers occupied ports, unhealthy PostgreSQL, password mismatch, Flyway
failure, and invalid or retained bootstrap settings. The
[database migration guide](../database/migrations.md) owns schema rules and the
narrowly scoped disposable reset.

Run unit tests during development:

```powershell
.\mvnw.cmd -B test
```

With Docker Engine running, run PostgreSQL integration tests before submitting
database or persistence changes:

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```
