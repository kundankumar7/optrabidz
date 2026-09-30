# OptraBidz

OptraBidz is a modular Spring Boot marketplace for coordinating startup
funding listings, investor bids, agreements, settlement outcomes, repayments,
notifications, and audit records.

It is a coordination platform, not a lender, broker, escrow service, credit
scoring engine, or real-money payment processor. Payment and notification
integrations in this repository are local or sandbox implementations.

## Run Locally

Local development requires Java 21, Git, and PostgreSQL 16. Docker Compose is
the recommended way to run PostgreSQL locally, while an existing compatible
native PostgreSQL 16 installation is also supported. The Maven wrapper is
included, so a separate Maven installation is not required.

Follow [Getting Started](docs/getting-started/README.md) for the complete path
from a fresh clone to a verified local administrator session. The local
Compose service and ignored `.env` file are development conveniences, not the
production deployment model.

## Architecture

[![OptraBidz system context](docs/architecture/assets/system-context.svg)](docs/architecture/README.md)

OptraBidz runs as one deployable modular monolith. PostgreSQL stores durable
state, Flyway owns schema changes, and a transactional outbox starts reliable
audit and notification processing after business data commits.

## Documentation

Use the [documentation portal](docs/README.md) to navigate by task:

- [getting started](docs/getting-started/README.md)
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
