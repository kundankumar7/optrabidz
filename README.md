# OptraBidz

OptraBidz is a modular Spring Boot marketplace for coordinating startup
funding listings, investor bids, agreements, settlement outcomes, repayments,
notifications, and audit records.

It is a coordination platform, not a lender, broker, escrow service, credit
scoring engine, or real-money payment processor. Payment and notification
integrations in this repository are local or sandbox implementations.

## Run Locally

Prerequisites: Java 21, Docker, and a running Docker Engine. The Maven wrapper
is included.

Start PostgreSQL 16:

```powershell
docker run --name optrabidz-postgres -e POSTGRES_DB=optrabidz -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

Start the application with development-only integrations enabled:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Open Swagger UI at `http://localhost:8080/swagger-ui.html`. API
documentation is disabled by default outside the development profile.

## Architecture

[![OptraBidz system context](docs/architecture/assets/system-context.svg)](docs/architecture/README.md)

OptraBidz runs as one deployable modular monolith. PostgreSQL stores durable
state, Flyway owns schema changes, and a transactional outbox starts reliable
audit and notification processing after business data commits.

## Documentation

Use the [documentation portal](docs/README.md) to navigate by task:

- [system architecture](docs/architecture/README.md)
- [API and error contract](docs/api/README.md)
- [database design and migrations](docs/database/README.md)
- [security model](docs/security/README.md)
- [operations](docs/operations/README.md)
- [architecture decisions](docs/decisions/README.md)

## Verification

Run unit tests:

```powershell
.\mvnw.cmd -B test
```

With Docker Engine running, run the PostgreSQL integration suite:

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```
