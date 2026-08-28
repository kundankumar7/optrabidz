# Getting Started

[Back to the documentation portal](../README.md)

## Prerequisites

- Temurin or another compatible Java 21 distribution
- Docker Desktop with Docker Engine running
- Git

The repository includes the Maven wrapper, so a separate Maven installation is
not required.

## Start PostgreSQL

```powershell
docker run --name optrabidz-postgres -e POSTGRES_DB=optrabidz -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

The command creates a disposable local database. Do not reuse its example
credentials outside local development.

## Start OptraBidz

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The `dev` profile enables Swagger UI, local notification delivery, and sandbox
payment providers. Open `http://localhost:8080/swagger-ui/index.html` after the
application starts.

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
