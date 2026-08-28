# 0001: Use a Modular Monolith

**Status:** Accepted

## Context

The marketplace capabilities share transactions, data relationships, and a
small delivery team. Independently deployed services would add network
contracts, distributed consistency, deployment coordination, and operational
overhead before those costs solve a demonstrated scaling or ownership problem.

## Decision

Deploy one Spring Boot application while separating capabilities into explicit
modules with application, domain, and adapter responsibilities. Cross-module
side effects use events and the transactional outbox where delivery must follow
a committed transaction.

## Consequences

- Local transactions remain available for strongly related state changes.
- One deployment and database reduce operational overhead.
- Package boundaries and tests must prevent the monolith becoming tightly
  coupled.
- A module can be extracted later only after its ownership and contracts are
  stable enough to justify distributed-system cost.

## Alternatives Considered

- A layered monolith without capability boundaries was rejected because it
  encourages unrelated features to share implementation details.
- Immediate microservices were rejected because current scale and team
  ownership do not justify their operational and consistency costs.
