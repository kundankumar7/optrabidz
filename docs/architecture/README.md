# System Architecture

[Back to the documentation portal](../README.md)

OptraBidz is deployed as one Spring Boot process. Business capabilities remain
separated as modules inside that process, while PostgreSQL and the transactional
outbox provide durable state and reliable post-commit processing.

![OptraBidz clients cross the HTTP and security boundary into a modular monolith that stores state in PostgreSQL and dispatches audit and notification work through an outbox](assets/optrabidz-system-overview.svg)

[High-resolution PNG for Jira and offline review](assets/optrabidz-system-overview-jira.png)

The synchronous request path ends at committed application state. Audit and
notification side effects begin from committed outbox records, which prevents a
successful business transaction from depending on an external delivery channel.

## Capability Map

| Area | Modules | Responsibility |
|---|---|---|
| Identity and access | `security`, `identity`, `participation` | Authentication, accounts, sessions, roles, and participant profiles |
| Marketplace | `classification`, `marketplace`, `governance` | Eligibility, listings, bids, agreements, and lifecycle rules |
| Finance | `financial` | Settlement, repayment, payment intents, provider attempts, and webhook processing |
| Supporting capabilities | `notification`, `audit`, `common`, `documentation` | Delivery, traceability, domain events, shared contracts, outbox infrastructure, and API-document adapters |

Controllers adapt HTTP requests. Application services coordinate use cases;
domain rules protect state transitions; repositories and integration adapters
handle infrastructure. Modules should depend on public contracts rather than
another module's persistence implementation.

The package structure implements these capability boundaries. Automated
architecture rules currently protect the exception, documentation, and webhook
boundaries; complete repository-wide module dependency enforcement is not yet
implemented.

## Persistence and Event Delivery

1. Flyway migrates PostgreSQL before Hibernate validates entity mappings.
2. A business transaction stores its state change and outbox event atomically.
3. The outbox dispatcher locks committed work and invokes registered
   processors.
4. Audit records are persisted and notification deliveries enter their own
   retry lifecycle.

The database is the current durable queue. Kafka, Redis, and external delivery
providers are not part of the implemented architecture.

Read the [architecture decisions](../decisions/README.md) for the reasoning
behind the modular monolith, Flyway, outbox, and error-contract boundaries.

See the [diagram publication guide](diagram-publication.md) for source and
fallback maintenance rules.
