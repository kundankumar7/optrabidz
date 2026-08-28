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

See the [diagram publication guide](diagram-publication.md) for source and
fallback maintenance rules.
