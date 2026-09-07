# Getting Started

[Back to the documentation portal](../README.md)

## Prerequisites

- Temurin or another compatible Java 21 distribution
- Docker Desktop with Docker Engine running
- Git

The repository includes the Maven wrapper, so a separate Maven installation is
not required.

## Create Local Configuration

Copy the tracked template to the ignored local file:

```powershell
Copy-Item .env.example .env
```

Fill in the three datasource variables. Keep all bootstrap, recovery, local
payment, sandbox payment, and webhook switches set to `false`; leave their
password, token, and webhook-secret fields blank.

The `dev` profile is the only profile that imports `.env`. Production does not
load this file.

## Start PostgreSQL

Choose a disposable local password, use it for the container, and place the
same value in `OPTRABIDZ_DATASOURCE_PASSWORD` inside `.env`:

```powershell
$localDbPassword = Read-Host "Local PostgreSQL password"
docker run --name optrabidz-postgres -e POSTGRES_DB=optrabidz -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=$localDbPassword -p 5432:5432 -d postgres:16
```

The command creates a disposable local database. Do not reuse its password
outside local development.

## Start OptraBidz

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The `dev` profile enables Swagger UI but leaves administrator bootstrap,
administrator recovery, local payment, sandbox payment, and payment webhooks
disabled. Sandbox email and push strategies are enabled by baseline
configuration and can be disabled by channel properties. Open
`http://localhost:8080/swagger-ui.html` after the application starts.

Enable privileged or simulated capabilities only for the operation being
performed, then disable them and remove their secret material. Follow the
[operations guide](../operations/README.md) instead of retaining an enable
flag in `.env`.

The default configuration is fail-closed for API documentation and local
integration adapters. The `prod` profile also requires datasource environment
variables; see the [operations guide](../operations/README.md).

## Verify the Project

Run the fast test suite during development:

```powershell
.\mvnw.cmd -B test
```

Run PostgreSQL integration tests before submitting a database or persistence
change:

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```

For schema ownership and safe local reset instructions, read the
[database migration guide](../database/migrations.md).
